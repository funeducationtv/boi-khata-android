package com.boikhata.data.repository

import android.app.Activity
import com.boikhata.data.local.CloudSyncDao
import com.boikhata.domain.model.CloudAuthResult
import com.boikhata.domain.model.CloudAuthState
import com.boikhata.domain.model.CloudSyncState
import com.boikhata.domain.repository.FirebaseAuthRepository
import com.boikhata.presentation.SessionManager
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor(
    private val cloudSyncDao: CloudSyncDao,
    private val sessionManager: SessionManager
) : FirebaseAuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Observe cloud sync state to keep session manager in sync
        scope.launch {
            cloudSyncDao.getCloudSyncState().collect { state ->
                val fbUser = auth.currentUser
                if (state != null && fbUser != null) {
                    sessionManager.setCloudAuthState(
                        CloudAuthState(
                            isLoggedIn = true,
                            uid = fbUser.uid,
                            phone = state.cloudPhone ?: fbUser.phoneNumber,
                            tenantId = state.tenantId.ifBlank { null },
                            role = state.cloudRole,
                            isPendingActivation = state.isPendingActivation
                        )
                    )
                } else if (fbUser != null) {
                    sessionManager.setCloudAuthState(
                        CloudAuthState(
                            isLoggedIn = true,
                            uid = fbUser.uid,
                            phone = fbUser.phoneNumber,
                            tenantId = null,
                            role = null,
                            isPendingActivation = true
                        )
                    )
                } else {
                    sessionManager.clearCloudAuth()
                }
            }
        }
    }

    override fun getCloudAuthState(): Flow<CloudAuthState> {
        return cloudSyncDao.getCloudSyncState().map { state ->
            val user = auth.currentUser
            if (user != null && state != null) {
                CloudAuthState(
                    isLoggedIn = true,
                    uid = user.uid,
                    phone = state.cloudPhone ?: user.phoneNumber,
                    tenantId = state.tenantId.ifBlank { null },
                    role = state.cloudRole,
                    isPendingActivation = state.isPendingActivation
                )
            } else if (user != null) {
                CloudAuthState(
                    isLoggedIn = true,
                    uid = user.uid,
                    phone = user.phoneNumber,
                    tenantId = null,
                    role = null,
                    isPendingActivation = true
                )
            } else {
                CloudAuthState(isLoggedIn = false)
            }
        }
    }

    override suspend fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (errorMessageBn: String) -> Unit
    ) = withContext(Dispatchers.Main) {
        val formattedPhone = formatBangladeshPhone(phoneNumber)
        if (formattedPhone == null) {
            onError("সঠিক ১১ ডিজিটের ফোন নম্বর দিন (যেমন: ০১৭XXXXXXXX)")
            return@withContext
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification handled in background
                scope.launch {
                    try {
                        auth.signInWithCredential(credential).await()
                        refreshIdToken()
                    } catch (e: Exception) {
                        // ignore auto-verification error
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val errorBn = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "ভুল ফোন নম্বর দেওয়া হয়েছে।"
                    is FirebaseTooManyRequestsException -> "অনেকবার ভুল চেষ্টা করা হয়েছে। কিছুক্ষণ পর চেষ্টা করুন।"
                    is FirebaseNetworkException -> "ইন্টারনেট সংযোগ নেই। ইন্টারনেট ছাড়া ক্লাউডে লগইন সম্ভব নয়।"
                    else -> "ওটিপি পাঠাতে সমস্যা হয়েছে: ${e.localizedMessage ?: "পুনরায় চেষ্টা করুন"}"
                }
                onError(errorBn)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<CloudAuthResult> = withContext(Dispatchers.IO) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("User not found after sign in")
            refreshIdTokenInternal(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshIdToken(): Result<CloudAuthResult> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No cloud user logged in"))
        try {
            refreshIdTokenInternal(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshIdTokenInternal(user: FirebaseUser): Result<CloudAuthResult> {
        val tokenResult = user.getIdToken(true).await()
        val claims = tokenResult.claims
        val tenantId = claims["tenantId"] as? String
        val role = claims["role"] as? String
        val isPending = tenantId.isNullOrBlank() || role.isNullOrBlank()

        val existingSync = cloudSyncDao.getSyncStateDirect()
        val updatedState = CloudSyncState(
            id = "primary",
            tenantId = tenantId ?: existingSync?.tenantId ?: "",
            cloudPhone = user.phoneNumber,
            cloudRole = role,
            isPendingActivation = isPending,
            lastBackupAt = existingSync?.lastBackupAt,
            lastRestoreAt = existingSync?.lastRestoreAt,
            lastCatalogSyncAt = existingSync?.lastCatalogSyncAt,
            licenseExpiresAt = existingSync?.licenseExpiresAt,
            licenseState = existingSync?.licenseState ?: com.boikhata.domain.model.LicenseState.ACTIVE,
            updatedAt = System.currentTimeMillis()
        )
        cloudSyncDao.saveSyncState(updatedState)

        sessionManager.setCloudAuthState(
            CloudAuthState(
                isLoggedIn = true,
                uid = user.uid,
                phone = user.phoneNumber,
                tenantId = tenantId,
                role = role,
                isPendingActivation = isPending
            )
        )

        return Result.success(
            CloudAuthResult(
                uid = user.uid,
                phone = user.phoneNumber ?: "",
                tenantId = tenantId,
                role = role,
                isPendingActivation = isPending
            )
        )
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            cloudSyncDao.clearSyncState()
            sessionManager.clearCloudAuth()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatBangladeshPhone(phone: String): String? {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.startsWith("880") && digits.length == 13 -> "+$digits"
            digits.startsWith("01") && digits.length == 11 -> "+88$digits"
            digits.startsWith("1") && digits.length == 10 -> "+880$digits"
            else -> null
        }
    }
}
