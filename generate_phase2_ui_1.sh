#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata/presentation"
mkdir -p $PKG_DIR/auth $PKG_DIR/users

# AUTH VIEWMODEL
cat << 'INNER_EOF' > $PKG_DIR/auth/AuthViewModel.kt
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
INNER_EOF

# PIN PAD (Reusable)
cat << 'INNER_EOF' > $PKG_DIR/auth/PinPad.kt
package com.boikhata.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val bnDigits = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (j in 0..2) {
                    val digit = bnDigits[i * 3 + j]
                    PinButton(text = digit, onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(modifier = Modifier.size(64.dp))
            PinButton(text = "০", onClick = { onDigit("০") })
            PinButton(text = "⌫", onClick = onDelete, color = Color(0xFFBA1A1A))
        }
    }
}

@Composable
private fun PinButton(text: String, onClick: () -> Unit, color: Color = Color(0xFF1B1B1F)) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFFF3F4F6))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
INNER_EOF

# ONBOARDING SCREEN
cat << 'INNER_EOF' > $PKG_DIR/auth/OnboardingScreen.kt
package com.boikhata.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(viewModel: AuthViewModel) {
    var step by remember { mutableStateOf(1) }
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("বই খাতা - সেটআপ", fontSize = 24.sp, color = Color(0xFF0061A4))
        Spacer(modifier = Modifier.height(32.dp))

        if (step == 1) {
            OutlinedTextField(
                value = shopName, onValueChange = { shopName = it },
                label = { Text("দোকানের নাম") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = ownerName, onValueChange = { ownerName = it },
                label = { Text("মালিকের নাম") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (shopName.isNotBlank() && ownerName.isNotBlank()) step = 2 },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("পরবর্তী") }
        } else {
            Text("পিন সেট করুন (৪ সংখ্যা)", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("পিন: " + "*".repeat(pin.length), fontSize = 24.sp, letterSpacing = 8.sp)
            Text("নিশ্চিত করুন: " + "*".repeat(confirmPin.length), fontSize = 24.sp, letterSpacing = 8.sp)
            if (error != null) Text(error!!, color = Color.Red)
            Spacer(modifier = Modifier.height(24.dp))
            PinPad(
                onDigit = { d ->
                    val digit = d.replace("১","1").replace("২","2").replace("৩","3").replace("৪","4").replace("৫","5").replace("৬","6").replace("৭","7").replace("৮","8").replace("৯","9").replace("০","0")
                    if (pin.length < 4) pin += digit
                    else if (confirmPin.length < 4) confirmPin += digit
                },
                onDelete = {
                    if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                    else if (pin.isNotEmpty()) pin = pin.dropLast(1)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (pin.length == 4 && pin == confirmPin) {
                        viewModel.setupOwner(shopName, ownerName, pin)
                    } else {
                        viewModel.clearError()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = pin.length == 4 && pin == confirmPin
            ) { Text("সম্পন্ন করুন") }
        }
    }
}
INNER_EOF

# LOCK SCREEN
cat << 'INNER_EOF' > $PKG_DIR/auth/LockScreen.kt
package com.boikhata.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LockScreen(viewModel: AuthViewModel) {
    var pin by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val cooldown by viewModel.cooldownTime.collectAsState()
    var timeRemaining by remember { mutableStateOf(0L) }

    LaunchedEffect(cooldown) {
        while(true) {
            val remain = cooldown - System.currentTimeMillis()
            if (remain > 0) timeRemaining = remain / 1000 else timeRemaining = 0
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("লগইন করুন", fontSize = 24.sp, color = Color(0xFF0061A4))
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("*".repeat(pin.length), fontSize = 32.sp, letterSpacing = 16.sp, color = Color(0xFF1B1B1F))
        Spacer(modifier = Modifier.height(16.dp))
        
        if (timeRemaining > 0) {
            Text("লকড! $timeRemaining সেকেন্ড অপেক্ষা করুন", color = Color.Red)
        } else if (error != null) {
            Text(error!!, color = Color.Red)
        } else {
            Text(" ", color = Color.Transparent)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PinPad(
            onDigit = { d ->
                if (timeRemaining == 0L && pin.length < 4) {
                    val digit = d.replace("১","1").replace("২","2").replace("৩","3").replace("৪","4").replace("৫","5").replace("৬","6").replace("৭","7").replace("৮","8").replace("৯","9").replace("০","0")
                    pin += digit
                    if (pin.length == 4) {
                        viewModel.loginAny(pin)
                        pin = ""
                    }
                }
            },
            onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
        )
    }
}
INNER_EOF

# USER VIEWMODEL
cat << 'INNER_EOF' > $PKG_DIR/users/UserViewModel.kt
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
INNER_EOF

