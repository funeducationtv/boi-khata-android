package com.boikhata.presentation.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.LicenseState
import com.boikhata.util.BengaliUtils.toBn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val licenseInfo by viewModel.licenseInfo.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val message by viewModel.message.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("সাবস্ক্রিপশন ও লাইসেন্স", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← ব্যাক", fontSize = 16.sp, color = Color(0xFF0061A4))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plan Card
            val expiryDateStr = licenseInfo?.expiresAt?.let {
                SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(it))
            } ?: "৩০ দিন পর"

            val state = licenseInfo?.state ?: LicenseState.ACTIVE
            val days = licenseInfo?.daysRemaining ?: 30

            val badgeColor = when (state) {
                LicenseState.ACTIVE -> Color(0xFF059669)
                LicenseState.GRACE -> Color(0xFFD97706)
                LicenseState.SOFT_LOCKED -> Color(0xFFBA1A1A)
                LicenseState.SUSPENDED -> Color(0xFF4B5563)
            }

            val badgeText = when (state) {
                LicenseState.ACTIVE -> "সক্রিয় (Active)"
                LicenseState.GRACE -> "গ্রেস পিরিয়ড (${days.toBn()} দিন বাকি)"
                LicenseState.SOFT_LOCKED -> "সফট-লক (কেবলমাত্র দেখা ও রিপোর্ট উন্মুক্ত)"
                LicenseState.SUSPENDED -> "স্থগিত (Suspended)"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বর্তমান প্ল্যান: Lite", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
                        Text("৳২৫০ / মাস", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Text("মেয়াদ উত্তীর্ণের তারিখ: $expiryDateStr", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
            }

            // Payment Instructions Card (Manual Phase 0a)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳 bKash পেমেন্ট নির্দেশিকা (ম্যানুয়াল)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF166534))
                    Text("১. আপনার বিকাশ অ্যাপ থেকে Send Money বা Cash In করুন।", fontSize = 13.sp, color = Color(0xFF14532D))
                    Text("২. ভেন্ডর বিকাশ নম্বর: ০১৭১১-৪৬৮০২৭", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF166534))
                    Text("৩. পরিমাণ: ৳২৫০", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF166534))
                    Text("৪. রেফারেন্স: আপনার দোকানের রেজিস্টার্ড মোবাইল নম্বর দিন।", fontSize = 13.sp, color = Color(0xFF14532D))

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("পেমেন্ট রেকর্ড করুন")
                    }

                    Text(
                        "নোট: পেমেন্ট তথ্য জমা দেওয়ার পর ভেন্ডর যাচাই করে লাইসেন্স বৃদ্ধি করবেন।",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Payment History Section
            Text("পেমেন্টের ইতিহাস", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1E293B))

            if (payments.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("এখনও কোনো পেমেন্ট রেকর্ড পাওয়া যায়নি।", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                payments.forEach { pay ->
                    val payDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(pay.createdAt))
                    val statusBn = if (pay.status == "PENDING") "যাচাই চলছে (PENDING)" else "অনুমোদিত (APPROVED)"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("৳${pay.amount.toBn()} (${pay.method})", fontWeight = FontWeight.Bold)
                                Text("প্রেরক: ${pay.referencePhone} | তারিখ: $payDate", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(
                                statusBn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pay.status == "PENDING") Color(0xFFD97706) else Color(0xFF059669)
                            )
                        }
                    }
                }
            }
        }
    }

    // Submit Payment Dialog
    if (showPaymentDialog) {
        var senderPhone by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("বিকাশ পেমেন্টের তথ্য দিন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("প্ল্যান মূল্য: ৳২৫০", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    OutlinedTextField(
                        value = senderPhone,
                        onValueChange = { senderPhone = it },
                        label = { Text("যে বিকাশ নম্বর থেকে পাঠিয়েছেন") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("TrxID বা নোট (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (senderPhone.isNotBlank()) {
                            viewModel.recordManualPayment(senderPhone, notes)
                            showPaymentDialog = false
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("জমা দিন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
