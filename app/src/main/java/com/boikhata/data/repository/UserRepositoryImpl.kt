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
        auditRepo.logAction(user.id, AuditAction.USER_CREATED, "Created user: \$name (\$role)")
        return Result.success(Unit)
    }

    override suspend fun disableUser(userId: String): Result<Unit> {
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        if (user.role == Role.OWNER) return Result.failure(Exception("Cannot disable owner"))
        userDao.updateUser(user.copy(isActive = false))
        auditRepo.logAction(user.id, AuditAction.USER_DISABLED, "Disabled user: \${user.name}")
        return Result.success(Unit)
    }

    override suspend fun resetPin(userId: String, ownerPin: String, newPin: String): Result<Unit> {
        val owner = userDao.getOwner() ?: return Result.failure(Exception("Owner not found"))
        val ownerHash = CryptoUtils.hashPin(ownerPin, owner.salt)
        if (ownerHash != owner.pinHash) return Result.failure(Exception("ভুল ওনার পিন"))
        
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPin(newPin, salt)
        userDao.updateUser(user.copy(pinHash = hash, salt = salt))
        auditRepo.logAction(user.id, AuditAction.PIN_RESET, "Reset PIN for user: \${user.name}")
        return Result.success(Unit)
    }
}
