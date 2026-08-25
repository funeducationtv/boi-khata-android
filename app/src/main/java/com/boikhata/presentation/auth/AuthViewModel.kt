package com.boikhata.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.repository.AuthRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    val sessionManager: SessionManager
) : ViewModel() {

    val hasOwner = authRepo.getOwnerCount().map { it > 0 }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _wrongAttempts = MutableStateFlow(0)
    val wrongAttempts: StateFlow<Int> = _wrongAttempts

    private val _cooldownTime = MutableStateFlow(0L)
    val cooldownTime: StateFlow<Long> = _cooldownTime

    fun setupOwner(shopName: String, ownerName: String, pin: String) {
        viewModelScope.launch {
            authRepo.setupOwner(shopName, ownerName, pin).onSuccess {
                loginAny(pin)
            }.onFailure { _error.value = it.message }
        }
    }

    fun loginAny(pin: String) {
        if (System.currentTimeMillis() < _cooldownTime.value) {
            _error.value = "Wait for cooldown"
            return
        }
        viewModelScope.launch {
            authRepo.loginAnyUser(pin).onSuccess { user ->
                _wrongAttempts.value = 0
                _error.value = null
                sessionManager.setSession(user)
            }.onFailure {
                val attempts = _wrongAttempts.value + 1
                _wrongAttempts.value = attempts
                _error.value = "ভুল পিন"
                if (attempts >= 5) {
                    _cooldownTime.value = System.currentTimeMillis() + 30000
                    _error.value = "৩০ সেকেন্ড অপেক্ষা করুন"
                }
            }
        }
    }
    
    fun clearError() { _error.value = null }
}
