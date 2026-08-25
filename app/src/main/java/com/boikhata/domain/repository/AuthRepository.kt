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
