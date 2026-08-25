package com.boikhata.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.Role
import com.boikhata.domain.repository.AuthRepository
import com.boikhata.domain.repository.UserRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    val sessionManager: SessionManager
) : ViewModel() {
    val users = userRepository.getUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addUser(name: String, role: Role, pin: String) {
        viewModelScope.launch { userRepository.addUser(name, role, pin) }
    }

    fun disableUser(userId: String) {
        viewModelScope.launch { userRepository.disableUser(userId) }
    }
    
    fun switchRole(userId: String, pin: String) {
        viewModelScope.launch { 
            authRepository.switchRole(userId, pin).onSuccess {
                sessionManager.setSession(it)
            }
        }
    }
}
