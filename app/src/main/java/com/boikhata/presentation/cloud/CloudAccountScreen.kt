package com.boikhata.presentation.cloud

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.Role
import com.boikhata.domain.repository.BackupProgress
import com.boikhata.domain.repository.RestoreProgress
import com.boikhata.util.BengaliUtils.toBn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: CloudAccountViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val verificationId by viewModel.verificationId.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val restoreProgress by viewModel.restoreProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    val currentUser by viewModel.sessionManager.currentUser.collectAsState()
    val isOwner = currentUser?.role == Role.OWNER
    val context = LocalContext.current
    val activity = context as? Activity

    var phoneNumberInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("ক্লাউড অ্যাকাউন্ট ও ব্যাকআপ", fontWeight = FontWeight.Bold) },
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
        if (!isOwner) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("ক্লাউড ব্যাকআপ কেবল দোকান মালিকের জন্য উন্মুক্ত।", color = Color.Gray)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!authState.isLoggedIn) {
                // 1. Login State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ক্লাউড লগইন (মালিকের ফোন)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
                        Text(
                            "ফোন হারিয়ে গেলেও আপনার খাতা ও বিক্রির ডাটা সুরক্ষিত থাকবে। ওটিপি (OTP) দিয়ে ক্লাউড চালু করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )

                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { phoneNumberInput = it },
                            label = { Text("মোবাইল নম্বর (যেমন: 017XXXXXXXX)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = verificationId == null
                        )

                        if (verificationId == null) {
                            Button(
                                onClick = {
                                    if (activity != null && phoneNumberInput.isNotBlank()) {
                                        viewModel.sendOtp(phoneNumberInput, activity)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ওটিপি (OTP) পাঠান")
                            }
                        } else {
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { if (it.length <= 6) otpInput = it },
                                label = { Text("৬ ডিজিটের ওটিপি কোড দিন") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (timerSeconds > 0) {
                                    Text("পুনরায় পাঠাতে অপেক্ষা: ${timerSeconds.toBn()} সেকেন্ড", fontSize = 12.sp, color = Color.Gray)
                                } else {
                                    TextButton(onClick = {
                                        if (activity != null) viewModel.sendOtp(phoneNumberInput, activity)
                                    }) {
                                        Text("কোড আবার পাঠান", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.verifyOtp(otpInput) },
                                    enabled = otpInput.length == 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("যাচাই করুন")
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "ইন্টারনেট না থাকলেও লোকাল পিন দিয়ে অ্যাপ ১০০% সচল থাকবে।",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            } else if (authState.isPendingActivation) {
                // 2. Pending Activation State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⏳ অ্যাকাউন্ট অনুমোদনের অপেক্ষায়", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF92400E))
                        Text(
                            "আপনার ফোন নম্বর (${authState.phone ?: ""}) দিয়ে ক্লাউড অ্যাকাউন্ট তৈরি হয়েছে। এটি সক্রিয় করতে অনুগ্রহ করে ভেন্ডরের সাথে যোগাযোগ করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF78350F)
                        )
                        Divider(color = Color(0xFFFDE68A))
                        Text("📞 ভেন্ডর যোগাযোগ: ০১৭১১-৪৬৮০২৭", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        Text("✉️ ইমেইল: mraaisa@gmail.com", color = Color(0xFF92400E), fontSize = 13.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.refreshClaims() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("স্ট্যাটাস চেক করুন")
                            }
                            OutlinedButton(
                                onClick = { viewModel.logoutFromCloud() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("লগআউট")
                            }
                        }
                    }
                }
            } else {
                // 3. Active Connected Cloud State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️ ক্লাউড সংযুক্ত", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                            Box(modifier = Modifier.background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("সক্রিয়", color = Color(0xFF065F46), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("দোকান আইডি: ${authState.tenantId ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text("সংযুক্ত মোবাইল: ${authState.phone ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text("ভূমিকা: ${authState.role ?: "OWNER"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                        val backupDateFormatted = lastBackupTime?.let {
                            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(it))
                        } ?: "এখনও ব্যাকআপ নেওয়া হয়নি"

                        Text("সর্বশেষ ক্লাউড ব্যাকআপ: $backupDateFormatted", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Backup Action
                        Button(
                            onClick = { viewModel.performBackup() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("এখনই ব্যাকআপ করুন ☁️")
                        }

                        // Restore Action
                        OutlinedButton(
                            onClick = { showRestoreConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ক্লাউড থেকে ডাটা ফিরিয়ে আনুন 📥")
                        }

                        // Logout Action
                        TextButton(
                            onClick = { viewModel.logoutFromCloud() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("ক্লাউড থেকে লগআউট করুন", color = Color(0xFFBA1A1A))
                        }
                    }
                }

                // Backup Progress Card
                when (val bp = backupProgress) {
                    is BackupProgress.BackingUp -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(bp.stageBn, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                LinearProgressIndicator(progress = { bp.percent }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    is BackupProgress.Success -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
                            Text(bp.messageBn, modifier = Modifier.padding(14.dp), color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                        }
                    }
                    is BackupProgress.Error -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                            Text(bp.messageBn, modifier = Modifier.padding(14.dp), color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }

                // Restore Progress Card
                when (val rp = restoreProgress) {
                    is RestoreProgress.Restoring -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(rp.stageBn, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                LinearProgressIndicator(progress = { rp.percent }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    is RestoreProgress.Success -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
                            Text(rp.messageBn, modifier = Modifier.padding(14.dp), color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                        }
                    }
                    is RestoreProgress.Error -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                            Text(rp.messageBn, modifier = Modifier.padding(14.dp), color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("ক্লাউড থেকে ডাটা ফিরিয়ে আনবেন?") },
            text = {
                Text("ক্লাউডে থাকা বিল, স্টক এবং খাতার ডাটা এই ডিভাইসে যোগ হবে। আপনি কি নিশ্চিত?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.performRestore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                ) { Text("হ্যাঁ, ফিরিয়ে আনুন") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
