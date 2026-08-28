package com.boikhata.security

import com.boikhata.data.local.CloudSyncDao
import com.boikhata.domain.model.LicenseBlockedException
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.LicenseStateCalculator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces the SOFT_LOCKED / SUSPENDED write-block at the data layer.
 *
 * - GRACE and ACTIVE are full access.
 * - SOFT_LOCKED / SUSPENDED block writes only; reads/exports are never blocked (never-lock rule).
 * - No cloud license yet (offline-first / pre-first-sync) defaults to the Phase-1 seeded
 *   GRACE state, so a fresh install is never write-blocked.
 */
@Singleton
class LicenseWriteGuard @Inject constructor(
    private val cloudSyncDao: CloudSyncDao
) {
    /** Throws [LicenseBlockedException] if the current license state blocks writes. */
    suspend fun assertWritable() {
        val state = currentState() ?: return
        if (state == LicenseState.SOFT_LOCKED || state == LicenseState.SUSPENDED) {
            throw LicenseBlockedException(state)
        }
    }

    /** Current effective license state, derived fresh from the stored expiry (never stale). */
    suspend fun currentState(): LicenseState? {
        val expiresAt = cloudSyncDao.getSyncStateDirect()?.licenseExpiresAt ?: return null
        return LicenseStateCalculator.derive(expiresAt)
    }
}
