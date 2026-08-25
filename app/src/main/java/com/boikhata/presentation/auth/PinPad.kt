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
