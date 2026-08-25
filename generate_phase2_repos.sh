#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# AUDIT REPO
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/AuditRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.AuditAction

interface AuditRepository {
    suspend fun logAction(userId: String, action: AuditAction, detail: String)
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/AuditRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.AuditLogDao
import com.boikhata.domain.model.AuditAction
import com.boikhata.domain.model.LocalAuditLog
import com.boikhata.domain.repository.AuditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class AuditRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao
) : AuditRepository {
    override suspend fun logAction(userId: String, action: AuditAction, detail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            auditLogDao.insert(
                LocalAuditLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    action = action,
                    detail = detail
                )
            )
        }
    }
}
INNER_EOF

# USER REPO
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/UserRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.Role
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun addUser(name: String, role: Role, pin: String): Result<Unit>
    suspend fun disableUser(userId: String): Result<Unit>
    suspend fun resetPin(userId: String, newPin: String): Result<Unit>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/UserRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.UserDao
import com.boikhata.domain.model.AuditAction
import com.boikhata.domain.model.Role
import com.boikhata.domain.model.User
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.UserRepository
import com.boikhata.util.CryptoUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val auditRepo: AuditRepository
) : UserRepository {
    override fun getUsers(): Flow<List<User>> = userDao.getAllUsers()

    override suspend fun addUser(name: String, role: Role, pin: String): Result<Unit> {
        if (role == Role.OWNER) return Result.failure(Exception("Cannot add multiple owners"))
        val count = userDao.getNonOwnerCount()
        if (count >= 3) return Result.failure(Exception("Max 3 staff users allowed in Lite tier"))

        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPin(pin, salt)
        val user = User(
            id = UUID.randomUUID().toString(),
            tenantId = "t_1",
            name = name,
            phone = "",
            role = role,
            pinHash = hash,
            salt = salt,
            isActive = true
        )
        userDao.insertUser(user)
        auditRepo.logAction(user.id, AuditAction.USER_CREATED, "Created user: $name ($role)")
        return Result.success(Unit)
    }

    override suspend fun disableUser(userId: String): Result<Unit> {
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        if (user.role == Role.OWNER) return Result.failure(Exception("Cannot disable owner"))
        userDao.updateUser(user.copy(isActive = false))
        auditRepo.logAction(user.id, AuditAction.USER_DISABLED, "Disabled user: ${user.name}")
        return Result.success(Unit)
    }

    override suspend fun resetPin(userId: String, newPin: String): Result<Unit> {
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPin(newPin, salt)
        userDao.updateUser(user.copy(pinHash = hash, salt = salt))
        auditRepo.logAction(user.id, AuditAction.PIN_RESET, "Reset PIN for user: ${user.name}")
        return Result.success(Unit)
    }
}
INNER_EOF

# AUTH REPO
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/AuthRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getOwnerCount(): Flow<Int>
    suspend fun setupOwner(shopName: String, ownerName: String, pin: String): Result<Unit>
    suspend fun loginOwner(pin: String): Result<User>
    suspend fun loginAnyUser(pin: String): Result<User>
    suspend fun switchRole(targetUserId: String, pin: String): Result<User>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/AuthRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.TenantDao
import com.boikhata.data.local.UserDao
import com.boikhata.domain.model.AuditAction
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.Role
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.AuthRepository
import com.boikhata.util.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val tenantDao: TenantDao,
    private val auditRepo: AuditRepository
) : AuthRepository {
    override fun getOwnerCount(): Flow<Int> = userDao.getOwnerCountFlow()

    override suspend fun setupOwner(shopName: String, ownerName: String, pin: String): Result<Unit> {
        val count = userDao.getOwnerCountFlow().firstOrNull() ?: 0
        if (count > 0) return Result.failure(Exception("Owner already exists"))

        val tenant = Tenant(
            id = "t_1",
            name = shopName,
            licenseState = LicenseState.GRACE,
            licenseDaysRemaining = 14
        )
        tenantDao.insertTenant(tenant)

        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPin(pin, salt)
        val owner = User(
            id = UUID.randomUUID().toString(),
            tenantId = "t_1",
            name = ownerName,
            phone = "",
            role = Role.OWNER,
            pinHash = hash,
            salt = salt,
            isActive = true
        )
        userDao.insertUser(owner)
        auditRepo.logAction(owner.id, AuditAction.USER_CREATED, "Owner setup complete")
        return Result.success(Unit)
    }

    override suspend fun loginOwner(pin: String): Result<User> {
        val owner = userDao.getOwner() ?: return Result.failure(Exception("Owner not found"))
        return verifyAndLog(owner, pin, AuditAction.LOGIN_SUCCESS, AuditAction.LOGIN_FAIL)
    }

    override suspend fun loginAnyUser(pin: String): Result<User> {
        val users = userDao.getAllUsers().firstOrNull() ?: emptyList()
        for (user in users.filter { it.isActive }) {
            val hash = CryptoUtils.hashPin(pin, user.salt)
            if (hash == user.pinHash) {
                auditRepo.logAction(user.id, AuditAction.LOGIN_SUCCESS, "Logged in")
                return Result.success(user)
            }
        }
        val owner = userDao.getOwner()
        if (owner != null) auditRepo.logAction(owner.id, AuditAction.LOGIN_FAIL, "Failed login attempt on PIN pad")
        return Result.failure(Exception("Invalid PIN"))
    }

    override suspend fun switchRole(targetUserId: String, pin: String): Result<User> {
        val targetUser = userDao.getUserById(targetUserId) ?: return Result.failure(Exception("User not found"))
        if (!targetUser.isActive) return Result.failure(Exception("User is disabled"))
        return verifyAndLog(targetUser, pin, AuditAction.ROLE_SWITCH, AuditAction.LOGIN_FAIL)
    }

    private suspend fun verifyAndLog(user: User, pin: String, successAction: AuditAction, failAction: AuditAction): Result<User> {
        val hash = CryptoUtils.hashPin(pin, user.salt)
        if (hash == user.pinHash) {
            auditRepo.logAction(user.id, successAction, "PIN verified")
            return Result.success(user)
        }
        auditRepo.logAction(user.id, failAction, "Invalid PIN attempt")
        return Result.failure(Exception("Invalid PIN"))
    }
}
INNER_EOF

# DASHBOARD REPO (DATA-LAYER FILTERING STRICTLY APPLIED)
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/DashboardRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.LicenseState
import kotlinx.coroutines.flow.Flow

sealed interface DashboardData {
    data class Owner(
        val tenantName: String,
        val licenseState: LicenseState,
        val licenseDaysRemaining: Int,
        val todaySales: Int,
        val todayCollection: Int,
        val todayProfit: Int,
        val lowStockCount: Int,
        val collectTodayList: List<CollectItem>
    ) : DashboardData

    data class Manager(
        val tenantName: String,
        val todaySales: Int,
        val lowStockCount: Int,
        val todayProfit: Int
    ) : DashboardData

    data class Sales(
        val tenantName: String,
        val shiftSales: Int,
        val cashDrawer: Int
    ) : DashboardData

    data class Accountant(
        val tenantName: String,
        val expenseSummary: Int,
        val cashbookNogad: Int,
        val cashbookBkash: Int,
        val cashbookBank: Int,
        val vatStatus: String
    ) : DashboardData
}

data class CollectItem(
    val id: String,
    val name: String,
    val amount: Int,
    val daysOverdue: Int,
    val isCritical: Boolean
)

interface DashboardRepository {
    fun getDashboardData(): Flow<DashboardData?>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/MockDashboardRepository.kt
package com.boikhata.data.repository

import com.boikhata.data.local.TenantDao
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.Role
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import com.boikhata.presentation.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class MockDashboardRepository @Inject constructor(
    private val tenantDao: TenantDao,
    private val sessionManager: SessionManager
) : DashboardRepository {
    override fun getDashboardData(): Flow<DashboardData?> {
        return combine(tenantDao.getTenant(), sessionManager.currentUser) { tenant, user ->
            if (user == null) return@combine null
            val tName = tenant?.name ?: "বই ঘর লাইব্রেরি ▾"
            val lState = tenant?.licenseState ?: LicenseState.GRACE
            val lDays = tenant?.licenseDaysRemaining ?: 8

            when (user.role) {
                Role.OWNER -> DashboardData.Owner(
                    tenantName = tName,
                    licenseState = lState,
                    licenseDaysRemaining = lDays,
                    todaySales = 4500,
                    todayCollection = 3200,
                    todayProfit = 1150,
                    lowStockCount = 3,
                    collectTodayList = listOf(
                        CollectItem("1", "রহিম ভাই", 2400, 30, true),
                        CollectItem("2", "করিম স্টোর", 1200, 15, false)
                    )
                )
                Role.MANAGER -> DashboardData.Manager(
                    tenantName = tName,
                    todaySales = 4500,
                    lowStockCount = 3,
                    todayProfit = 1150
                )
                Role.SALES -> DashboardData.Sales(
                    tenantName = tName,
                    shiftSales = 1500, // Only own shift
                    cashDrawer = 1500
                )
                Role.ACCOUNTANT -> DashboardData.Accountant(
                    tenantName = tName,
                    expenseSummary = 800,
                    cashbookNogad = 4000,
                    cashbookBkash = 20000,
                    cashbookBank = 50000,
                    vatStatus = "Up to date"
                )
            }
        }
    }
}
INNER_EOF

# REPOSITORY MODULE BINDINGS
cat << 'INNER_EOF' > $PKG_DIR/di/RepositoryModule.kt
package com.boikhata.di

import com.boikhata.data.repository.AuditRepositoryImpl
import com.boikhata.data.repository.AuthRepositoryImpl
import com.boikhata.data.repository.MockDashboardRepository
import com.boikhata.data.repository.UserRepositoryImpl
import com.boikhata.domain.biometric.BiometricAuth
import com.boikhata.domain.biometric.BiometricAuthStub
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.AuthRepository
import com.boikhata.domain.repository.DashboardRepository
import com.boikhata.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDashboardRepository(impl: MockDashboardRepository): DashboardRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository
}

@Module
@InstallIn(SingletonComponent::class)
object BiometricModule {
    @Provides
    @Singleton
    fun provideBiometricAuth(): BiometricAuth = BiometricAuthStub()
}
INNER_EOF

