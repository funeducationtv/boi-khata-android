package com.boikhata.data.repository

import com.boikhata.data.local.CloudSyncDao
import com.boikhata.domain.model.LicenseInfo
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.LicenseStateCalculator
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
    private val cloudSyncDao: CloudSyncDao
) : CloudLicenseRepository {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override suspend fun syncLicense(tenantId: String): Result<LicenseInfo> = withContext(Dispatchers.IO) {
        if (tenantId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Tenant ID cannot be empty"))
        }

        // Live rules allow license_records read for OWNER only. Non-owners keep the
        // Room-cached state and never attempt the (denied) network read.
        val role = cloudSyncDao.getSyncStateDirect()?.cloudRole
        if (role != null && role != "OWNER") {
            val cached = cachedLicense(tenantId)
            return@withContext if (cached != null) Result.success(cached)
            else Result.failure(IllegalStateException("লাইসেন্স তথ্য শুধুমাত্র মালিকের জন্য উপলব্ধ"))
        }

        try {
            // Read-only from top-level /license_records/{tenantId}
            val snapshot = firestore.collection("license_records").document(tenantId).get().await()

            // MANDATORY existence check: a missing document throws NO exception, so the
            // catch block alone would wrongly fabricate a license. Missing -> local fallback.
            if (!snapshot.exists()) {
                val cached = cachedLicense(tenantId)
                return@withContext if (cached != null) Result.success(cached)
                else Result.failure(IllegalStateException("লাইসেন্স রেকর্ড পাওয়া যায়নি"))
            }

            // expiresAt is written by vendor scripts as a Firestore Timestamp; fall back to
            // a numeric epoch-millis Long for robustness (never fabricate now + 30d).
            val expiresAt = snapshot.getTimestamp("expiresAt")?.toDate()?.time
                ?: snapshot.getLong("expiresAt")
            val updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.time
                ?: snapshot.getLong("updatedAt")
                ?: System.currentTimeMillis()

            if (expiresAt == null) {
                val cached = cachedLicense(tenantId)
                return@withContext if (cached != null) Result.success(cached)
                else Result.failure(IllegalStateException("লাইসেন্সের মেয়াদ পাওয়া যায়নি"))
            }

            val derivedState = LicenseStateCalculator.derive(expiresAt)
            cloudSyncDao.updateLicense(expiresAt, derivedState, updatedAt)

            Result.success(
                LicenseInfo(
                    tenantId = tenantId,
                    expiresAt = expiresAt,
                    daysRemaining = LicenseStateCalculator.daysRemaining(expiresAt),
                    state = derivedState,
                    updatedAt = updatedAt
                )
            )
        } catch (e: Exception) {
            // Offline-first / network failure: keep the last known LOCAL state. Never surface
            // an "ERROR" UI state, and never fabricate an ACTIVE license.
            val cached = cachedLicense(tenantId)
            if (cached != null) Result.success(cached) else Result.failure(e)
        }
    }

    override fun observeLicenseState(): Flow<LicenseInfo?> {
        return cloudSyncDao.getCloudSyncState().map { state ->
            val expiresAt = state?.licenseExpiresAt ?: return@map null
            LicenseInfo(
                tenantId = state.tenantId,
                expiresAt = expiresAt,
                daysRemaining = LicenseStateCalculator.daysRemaining(expiresAt),
                state = LicenseStateCalculator.derive(expiresAt),
                updatedAt = state.updatedAt
            )
        }
    }

    /** Rebuild a [LicenseInfo] from the last known local Room state, deriving state fresh. */
    private suspend fun cachedLicense(tenantId: String): LicenseInfo? {
        val cached = cloudSyncDao.getSyncStateDirect() ?: return null
        val expiresAt = cached.licenseExpiresAt ?: return null
        val state: LicenseState = LicenseStateCalculator.derive(expiresAt)
        return LicenseInfo(
            tenantId = tenantId,
            expiresAt = expiresAt,
            daysRemaining = LicenseStateCalculator.daysRemaining(expiresAt),
            state = state,
            updatedAt = cached.updatedAt
        )
    }
}
