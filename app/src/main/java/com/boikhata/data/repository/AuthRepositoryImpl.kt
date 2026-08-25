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
