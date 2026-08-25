package com.boikhata.presentation

import com.boikhata.domain.model.CloudAuthState
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _cloudAuthState = MutableStateFlow(CloudAuthState())
    val cloudAuthState: StateFlow<CloudAuthState> = _cloudAuthState.asStateFlow()

    private var lastActivityTime = System.currentTimeMillis()

    fun setSession(user: User) {
        _currentUser.value = user
        _isLocked.value = false
        updateActivity()
    }

    fun setCloudAuthState(state: CloudAuthState) {
        _cloudAuthState.value = state
    }

    fun clearCloudAuth() {
        _cloudAuthState.value = CloudAuthState()
    }

    fun lock() {
        if (_currentUser.value != null) {
            _isLocked.value = true
        }
    }

    fun logout() {
        _currentUser.value = null
        _isLocked.value = true
    }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun checkAutoLock() {
        // 5 minutes auto-lock
        if (_currentUser.value != null && !_isLocked.value) {
            if (System.currentTimeMillis() - lastActivityTime > 5 * 60 * 1000) {
                lock()
            }
        }
    }
}
