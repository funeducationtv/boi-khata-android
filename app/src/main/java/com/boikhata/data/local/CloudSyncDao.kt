package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    // ---------------------------------------------------------------------------------
    // Upserts (C2): the previous @Query UPDATE variants were silent no-ops when no
    // 'primary' row existed yet. These ensure a row is present before mutating it.
    // ---------------------------------------------------------------------------------

    @Query("UPDATE cloud_sync_state SET lastBackupAt = :timestamp, updatedAt = :timestamp WHERE id = 'primary'")
    suspend fun setLastBackupAtRaw(timestamp: Long)

    @Query("UPDATE cloud_sync_state SET lastRestoreAt = :timestamp, updatedAt = :timestamp WHERE id = 'primary'")
    suspend fun setLastRestoreAtRaw(timestamp: Long)

    @Query("UPDATE cloud_sync_state SET lastCatalogSyncAt = :timestamp, updatedAt = :timestamp WHERE id = 'primary'")
    suspend fun setLastCatalogSyncAtRaw(timestamp: Long)

    @Query("UPDATE cloud_sync_state SET licenseExpiresAt = :expiresAt, licenseState = :state, updatedAt = :updatedAt WHERE id = 'primary'")
    suspend fun setLicenseRaw(expiresAt: Long, state: LicenseState, updatedAt: Long)

    @Query("DELETE FROM cloud_sync_state WHERE id = 'primary'")
    suspend fun clearSyncState()

    @Transaction
    suspend fun updateLastBackupAt(timestamp: Long) {
        if (getSyncStateDirect() == null) {
            saveSyncState(CloudSyncState(tenantId = "", cloudPhone = null, cloudRole = null, lastBackupAt = timestamp, updatedAt = timestamp))
        } else {
            setLastBackupAtRaw(timestamp)
        }
    }

    @Transaction
    suspend fun updateLastRestoreAt(timestamp: Long) {
        if (getSyncStateDirect() == null) {
            saveSyncState(CloudSyncState(tenantId = "", cloudPhone = null, cloudRole = null, lastRestoreAt = timestamp, updatedAt = timestamp))
        } else {
            setLastRestoreAtRaw(timestamp)
        }
    }

    @Transaction
    suspend fun updateLastCatalogSyncAt(timestamp: Long) {
        if (getSyncStateDirect() == null) {
            saveSyncState(CloudSyncState(tenantId = "", cloudPhone = null, cloudRole = null, lastCatalogSyncAt = timestamp, updatedAt = timestamp))
        } else {
            setLastCatalogSyncAtRaw(timestamp)
        }
    }

    @Transaction
    suspend fun updateLicense(expiresAt: Long, state: LicenseState, updatedAt: Long = System.currentTimeMillis()) {
        if (getSyncStateDirect() == null) {
            saveSyncState(
                CloudSyncState(
                    tenantId = "", cloudPhone = null, cloudRole = null,
                    licenseExpiresAt = expiresAt, licenseState = state, updatedAt = updatedAt
                )
            )
        } else {
            setLicenseRaw(expiresAt, state, updatedAt)
        }
    }

    // ---------------------------------------------------------------------------------
    // One-time tenant rebind (A4): move every local row tagged with the Phase-1/2/3
    // placeholder "t_1" to the claims tenantId, so backup/restore tenant-scoped queries
    // match the cloud writes. Runs once, on the first successful cloud login.
    // ---------------------------------------------------------------------------------

    @Query("UPDATE users SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindUsers(oldTenantId: String, newTenantId: String)

    @Query("UPDATE devices SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindDevices(oldTenantId: String, newTenantId: String)

    @Query("UPDATE books SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBooks(oldTenantId: String, newTenantId: String)

    @Query("UPDATE stock_ledger SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindStockLedger(oldTenantId: String, newTenantId: String)

    @Query("UPDATE bills SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBills(oldTenantId: String, newTenantId: String)

    @Query("UPDATE bill_lines SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBillLines(oldTenantId: String, newTenantId: String)

    @Query("UPDATE khata_customers SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindKhataCustomers(oldTenantId: String, newTenantId: String)

    @Query("UPDATE khata_entries SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindKhataEntries(oldTenantId: String, newTenantId: String)

    @Query("UPDATE expense_categories SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindExpenseCategories(oldTenantId: String, newTenantId: String)

    @Query("UPDATE expenses SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindExpenses(oldTenantId: String, newTenantId: String)

    @Query("UPDATE cashbook_entries SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindCashbookEntries(oldTenantId: String, newTenantId: String)

    @Query("UPDATE owner_drawings SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindOwnerDrawings(oldTenantId: String, newTenantId: String)

    @Transaction
    suspend fun rebindTenantId(oldTenantId: String, newTenantId: String) {
        if (oldTenantId.isBlank() || newTenantId.isBlank() || oldTenantId == newTenantId) return
        rebindUsers(oldTenantId, newTenantId)
        rebindDevices(oldTenantId, newTenantId)
        rebindBooks(oldTenantId, newTenantId)
        rebindStockLedger(oldTenantId, newTenantId)
        rebindBills(oldTenantId, newTenantId)
        rebindBillLines(oldTenantId, newTenantId)
        rebindKhataCustomers(oldTenantId, newTenantId)
        rebindKhataEntries(oldTenantId, newTenantId)
        rebindExpenseCategories(oldTenantId, newTenantId)
        rebindExpenses(oldTenantId, newTenantId)
        rebindCashbookEntries(oldTenantId, newTenantId)
        rebindOwnerDrawings(oldTenantId, newTenantId)
    }
}
