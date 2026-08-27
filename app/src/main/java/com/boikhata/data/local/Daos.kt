package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnNote(note: ReturnNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnNoteLines(lines: List<ReturnNoteLine>)

    @Query("SELECT * FROM return_notes WHERE tenantId = :tenantId ORDER BY returnDate DESC")
    fun getAllReturnNotes(tenantId: String): Flow<List<ReturnNoteWithLines>>

    @Query("SELECT * FROM return_notes WHERE tenantId = :tenantId")
    suspend fun getAllReturnNotesDirect(tenantId: String): List<ReturnNote>

    @Query("SELECT * FROM return_notes WHERE id = :id AND tenantId = :tenantId")
    suspend fun getReturnNoteById(tenantId: String, id: String): ReturnNote?

    @Query("SELECT * FROM return_note_lines WHERE returnNoteId = :returnNoteId")
    suspend fun getLinesForReturnNote(returnNoteId: String): List<ReturnNoteLine>

    @Query("UPDATE return_notes SET status = :status, updatedAt = :timestamp WHERE id = :noteId")
    suspend fun updateReturnNoteStatus(noteId: String, status: ReturnStatus, timestamp: Long)

    @Query("SELECT * FROM return_notes WHERE tenantId = :tenantId AND originalBillId = :billId")
    suspend fun getReturnsForBill(tenantId: String, billId: String): List<ReturnNote>
}

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
