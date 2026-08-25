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
