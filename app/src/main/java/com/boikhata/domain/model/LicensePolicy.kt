package com.boikhata.domain.model

/**
 * Central license lifecycle derivation shared by the license sync path,
 * the observable license flow, and the write guard.
 *
 * Lifecycle (LOCKED design): ACTIVE -> GRACE (+14d) -> SOFT_LOCKED (+30d) -> SUSPENDED.
 */
object LicenseStateCalculator {
    private const val GRACE_MS = 14L * 24 * 60 * 60 * 1000
    private const val SOFT_LOCK_MS = 30L * 24 * 60 * 60 * 1000
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun derive(expiresAt: Long, now: Long = System.currentTimeMillis()): LicenseState = when {
        now <= expiresAt -> LicenseState.ACTIVE
        now <= expiresAt + GRACE_MS -> LicenseState.GRACE
        now <= expiresAt + SOFT_LOCK_MS -> LicenseState.SOFT_LOCKED
        else -> LicenseState.SUSPENDED
    }

    fun daysRemaining(expiresAt: Long, now: Long = System.currentTimeMillis()): Int =
        ((expiresAt - now) / DAY_MS).toInt().coerceAtLeast(0)
}

/**
 * Thrown by the [com.boikhata.security.LicenseWriteGuard] when a write is attempted
 * while the license is SOFT_LOCKED or SUSPENDED. Reads/exports are never blocked.
 */
class LicenseBlockedException(
    val state: LicenseState
) : Exception("সাবস্ক্রিপশন মেয়াদোত্তীর্ণ — পেমেন্ট করে আবার চালু করুন")
