package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.domain.model.CloudSyncState
import com.boikhata.domain.model.LicenseState
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudSyncDao {
    @Query("SELECT * FROM cloud_sync_state WHERE id = 'primary' LIMIT 1")
    fun getCloudSyncState(): Flow<CloudSyncState?>

    @Query("SELECT * FROM cloud_sync_state WHERE id = 'primary' LIMIT 1")
    suspend fun getSyncStateDirect(): CloudSyncState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncState(state: CloudSyncState)

    @Query("UPDATE cloud_sync_state SET lastBackupAt = :timestamp, updatedAt = :timestamp WHERE id = 'primary'")
    suspend fun updateLastBackupAt(timestamp: Long)

    @Query("UPDATE cloud_sync_state SET lastRestoreAt = :timestamp, updatedAt = :timestamp WHERE id = 'primary'")
    suspend fun updateLastRestoreAt(timestamp: Long)

    @Query("UPDATE cloud_sync_state SET lastCatalogSyncAt = :timestamp, updatedAt = :timestamp WHERE id = 'primary'")
    suspend fun updateLastCatalogSyncAt(timestamp: Long)

    @Query("UPDATE cloud_sync_state SET licenseExpiresAt = :expiresAt, licenseState = :state, updatedAt = :updatedAt WHERE id = 'primary'")
    suspend fun updateLicense(expiresAt: Long, state: LicenseState, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM cloud_sync_state WHERE id = 'primary'")
    suspend fun clearSyncState()
}
