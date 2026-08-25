package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.domain.model.LocalAuditLog
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants LIMIT 1")
    fun getTenant(): Flow<Tenant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE tenantId = :tenantId AND isActive = 1")
    fun getUsersByTenant(tenantId: String): Flow<List<User>>
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE role = 'OWNER' LIMIT 1")
    suspend fun getOwner(): User?
    
    @Query("SELECT COUNT(*) FROM users WHERE role = 'OWNER'")
    fun getOwnerCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role != 'OWNER' AND isActive = 1")
    suspend fun getNonOwnerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(log: LocalAuditLog)
}
