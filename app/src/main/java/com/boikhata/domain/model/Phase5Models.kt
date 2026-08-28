package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_sync_state")
data class CloudSyncState(
    @PrimaryKey val id: String = "primary",
    val tenantId: String,
    val cloudPhone: String?,
    val cloudRole: String?,
    val isPendingActivation: Boolean = false,
    val lastBackupAt: Long? = null,
    val lastRestoreAt: Long? = null,
    val lastCatalogSyncAt: Long? = null,
    val licenseExpiresAt: Long? = null,
    val licenseState: LicenseState = LicenseState.ACTIVE,
    val updatedAt: Long = System.currentTimeMillis()
)

data class CloudAuthResult(
    val uid: String,
    val phone: String,
    val tenantId: String?,
    val role: String?,
    val isPendingActivation: Boolean
)

data class CloudAuthState(
    val isLoggedIn: Boolean = false,
    val uid: String? = null,
    val phone: String? = null,
    val tenantId: String? = null,
    val role: String? = null,
    val isPendingActivation: Boolean = false
)

data class LicenseInfo(
    val tenantId: String,
    val expiresAt: Long,
    val daysRemaining: Int,
    val state: LicenseState,
    val updatedAt: Long
)

data class SubscriptionPaymentRecord(
    val id: String,
    val tenantId: String,
    val amount: Double,
    val method: String,
    val status: String,
    val referencePhone: String,
    val trxId: String? = null,
    val note: String? = null,
    val createdAt: Long
)
