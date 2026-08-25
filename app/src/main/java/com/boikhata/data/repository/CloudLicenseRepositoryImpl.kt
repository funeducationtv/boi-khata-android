package com.boikhata.data.repository

import com.boikhata.data.local.CloudSyncDao
import com.boikhata.data.local.TenantDao
import com.boikhata.domain.model.LicenseInfo
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CloudLicenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudLicenseRepositoryImpl @Inject constructor(
    private val cloudSyncDao: CloudSyncDao,
    private val tenantDao: TenantDao
) : CloudLicenseRepository {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override suspend fun syncLicense(tenantId: String): Result<LicenseInfo> = withContext(Dispatchers.IO) {
        if (tenantId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Tenant ID cannot be empty"))
        }

        try {
            // Read-only from /license_records/{tenantId}
            val snapshot = firestore.collection("license_records").document(tenantId).get().await()
            val expiresAt = snapshot.getLong("expiresAt") ?: (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))
            val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()

            val now = System.currentTimeMillis()
            val diffMillis = expiresAt - now
            val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

            val derivedState = when {
                now <= expiresAt -> LicenseState.ACTIVE
                now <= expiresAt + (14L * 24 * 60 * 60 * 1000) -> LicenseState.GRACE
                now <= expiresAt + (30L * 24 * 60 * 60 * 1000) -> LicenseState.SOFT_LOCKED
                else -> LicenseState.SUSPENDED
            }

            // Save in local Room CloudSyncDao
            cloudSyncDao.updateLicense(expiresAt, derivedState, updatedAt)

            val licenseInfo = LicenseInfo(
                tenantId = tenantId,
                expiresAt = expiresAt,
                daysRemaining = daysRemaining,
                state = derivedState,
                updatedAt = updatedAt
            )
            Result.success(licenseInfo)
        } catch (e: Exception) {
            // Fallback to locally cached license info
            val cached = cloudSyncDao.getSyncStateDirect()
            if (cached?.licenseExpiresAt != null) {
                val expiresAt = cached.licenseExpiresAt
                val now = System.currentTimeMillis()
                val diffMillis = expiresAt - now
                val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                val state = when {
                    now <= expiresAt -> LicenseState.ACTIVE
                    now <= expiresAt + (14L * 24 * 60 * 60 * 1000) -> LicenseState.GRACE
                    now <= expiresAt + (30L * 24 * 60 * 60 * 1000) -> LicenseState.SOFT_LOCKED
                    else -> LicenseState.SUSPENDED
                }
                Result.success(
                    LicenseInfo(
                        tenantId = tenantId,
                        expiresAt = expiresAt,
                        daysRemaining = daysRemaining,
                        state = state,
                        updatedAt = cached.updatedAt
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    override fun observeLicenseState(): Flow<LicenseInfo?> {
        return cloudSyncDao.getCloudSyncState().map { state ->
            if (state?.licenseExpiresAt == null) return@map null
            val expiresAt = state.licenseExpiresAt
            val now = System.currentTimeMillis()
            val diffMillis = expiresAt - now
            val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            val derivedState = when {
                now <= expiresAt -> LicenseState.ACTIVE
                now <= expiresAt + (14L * 24 * 60 * 60 * 1000) -> LicenseState.GRACE
                now <= expiresAt + (30L * 24 * 60 * 60 * 1000) -> LicenseState.SOFT_LOCKED
                else -> LicenseState.SUSPENDED
            }
            LicenseInfo(
                tenantId = state.tenantId,
                expiresAt = expiresAt,
                daysRemaining = daysRemaining,
                state = derivedState,
                updatedAt = state.updatedAt
            )
        }
    }
}
