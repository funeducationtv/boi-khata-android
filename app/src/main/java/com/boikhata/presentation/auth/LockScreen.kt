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
