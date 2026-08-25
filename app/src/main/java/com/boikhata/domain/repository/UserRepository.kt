package com.boikhata.domain.repository

import com.boikhata.domain.model.Role
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun addUser(name: String, role: Role, pin: String): Result<Unit>
    suspend fun disableUser(userId: String): Result<Unit>
    suspend fun resetPin(userId: String, ownerPin: String, newPin: String): Result<Unit>
}
