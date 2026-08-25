package com.boikhata.presentation.cloud

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.CloudAuthState
import com.boikhata.domain.repository.*
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudAccountViewModel @Inject constructor(
    private val authRepo: FirebaseAuthRepository,
    private val backupRepo: BackupRepository,
    private val restoreRepo: RestoreRepository,
    private val licenseRepo: CloudLicenseRepository,
    val sessionManager: SessionManager
) : ViewModel() {

    val authState: StateFlow<CloudAuthState> = sessionManager.cloudAuthState

    private val _verificationId = MutableStateFlow<String?>(null)
    val verificationId: StateFlow<String?> = _verificationId.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _backupProgress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val backupProgress: StateFlow<BackupProgress> = _backupProgress.asStateFlow()

    private val _restoreProgress = MutableStateFlow<RestoreProgress>(RestoreProgress.Idle)
    val restoreProgress: StateFlow<RestoreProgress> = _restoreProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.getCloudAuthState().collect { state ->
                sessionManager.setCloudAuthState(state)
                if (state.isLoggedIn && !state.isPendingActivation && !state.tenantId.isNullOrBlank()) {
                    licenseRepo.syncLicense(state.tenantId)
                }
            }
        }
        viewModelScope.launch {
            backupRepo.getLastBackupTime().collect { _lastBackupTime.value = it }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun sendOtp(phone: String, activity: Activity) {
        viewModelScope.launch {
            _errorMessage.value = null
            authRepo.sendOtp(
                phoneNumber = phone,
                activity = activity,
                onCodeSent = { vId ->
                    _verificationId.value = vId
                    startResendTimer()
                },
                onError = { err ->
                    _errorMessage.value = err
                }
            )
        }
    }

    private fun startResendTimer() {
        _timerSeconds.value = 60
        viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _timerSeconds.value -= 1
            }
        }
    }

    fun verifyOtp(otp: String) {
        val vId = _verificationId.value ?: return
        viewModelScope.launch {
            _errorMessage.value = null
            val result = authRepo.verifyOtp(vId, otp)
            if (result.isFailure) {
                _errorMessage.value = "ভুল ওটিপি কোড। অনুগ্রহ করে সঠিক কোড দিন।"
            } else {
                val authRes = result.getOrNull()
                if (authRes != null && !authRes.isPendingActivation && !authRes.tenantId.isNullOrBlank()) {
                    licenseRepo.syncLicense(authRes.tenantId)
                }
            }
        }
    }

    fun refreshClaims() {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = authRepo.refreshIdToken()
            if (result.isFailure) {
                _errorMessage.value = "ক্লাউড স্ট্যাটাস রিফ্রেশে ত্রুটি।"
            } else {
                val authRes = result.getOrNull()
                if (authRes != null && !authRes.isPendingActivation && !authRes.tenantId.isNullOrBlank()) {
                    licenseRepo.syncLicense(authRes.tenantId)
                }
            }
        }
    }

    fun performBackup() {
        val tenantId = authState.value.tenantId ?: "t_1"
        viewModelScope.launch {
            backupRepo.performBackup(tenantId).collect {
                _backupProgress.value = it
            }
        }
    }

    fun performRestore() {
        val tenantId = authState.value.tenantId ?: "t_1"
        viewModelScope.launch {
            restoreRepo.performRestore(tenantId).collect {
                _restoreProgress.value = it
            }
        }
    }

    fun logoutFromCloud() {
        viewModelScope.launch {
            authRepo.logout()
            _verificationId.value = null
        }
    }
}
