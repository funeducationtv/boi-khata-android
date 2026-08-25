package com.boikhata.domain.repository

import android.app.Activity
import com.boikhata.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthRepository {
    fun getCloudAuthState(): Flow<CloudAuthState>
    suspend fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (errorMessageBn: String) -> Unit
    )
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<CloudAuthResult>
    suspend fun refreshIdToken(): Result<CloudAuthResult>
    suspend fun logout(): Result<Unit>
}

interface CloudLicenseRepository {
    suspend fun syncLicense(tenantId: String): Result<LicenseInfo>
    fun observeLicenseState(): Flow<LicenseInfo?>
}

interface SubscriptionRepository {
    suspend fun createPaymentRecord(
        tenantId: String,
        amount: Double = 250.0,
        referencePhone: String,
        notes: String = ""
    ): Result<SubscriptionPaymentRecord>
    fun getPaymentHistory(tenantId: String): Flow<List<SubscriptionPaymentRecord>>
}

sealed class BackupProgress {
    object Idle : BackupProgress()
    data class BackingUp(val stageBn: String, val percent: Float) : BackupProgress()
    data class Success(val messageBn: String, val timestamp: Long) : BackupProgress()
    data class Error(val messageBn: String) : BackupProgress()
}

sealed class RestoreProgress {
    object Idle : RestoreProgress()
    data class Restoring(val stageBn: String, val percent: Float) : RestoreProgress()
    data class Success(val messageBn: String) : RestoreProgress()
    data class Error(val messageBn: String) : RestoreProgress()
}

interface BackupRepository {
    fun performBackup(tenantId: String): Flow<BackupProgress>
    fun getLastBackupTime(): Flow<Long?>
}

interface RestoreRepository {
    suspend fun checkCloudDataAvailable(tenantId: String): Result<Boolean>
    fun performRestore(tenantId: String): Flow<RestoreProgress>
}

interface RemoteMasterCatalogRepository {
    fun checkForRemoteUpdates(): Flow<Boolean>
    suspend fun syncMasterCatalogFromCloud(): Result<Int>
}
