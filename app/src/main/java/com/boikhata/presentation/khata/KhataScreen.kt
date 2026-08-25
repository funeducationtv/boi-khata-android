package com.boikhata.presentation.khata

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.*
import com.boikhata.presentation.SessionManager
import com.boikhata.util.BengaliUtils.toBn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(sessionManager: SessionManager, viewModel: KhataViewModel = hiltViewModel()) {
    val customers by viewModel.customers.collectAsState()
    val user by sessionManager.currentUser.collectAsState()
    val message by viewModel.message.collectAsState()
    val isOwner = user?.role == Role.OWNER
    val isSales = user?.role == Role.SALES

    if (isSales) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)), contentAlignment = Alignment.Center) {
            Text("খাতা দেখার অনুমতি কেবলমাত্র দোকান মালিকের রয়েছে।", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
        return
    }

    var showAddCustomer by remember { mutableStateOf(false) }
    var selectedCustomerForPayment by remember { mutableStateOf<KhataCustomerWithBalance?>(null) }
    var selectedCustomerForLedger by remember { mutableStateOf<KhataCustomerWithBalance?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("খাতা (বকেয়া ও আদায়)", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
                    Text("মোট কাস্টমার: ${customers.size.toBn()} জন", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (isOwner) {
                    Button(
                        onClick = { showAddCustomer = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ নতুন কাস্টমার")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("কোনো খাতা কাস্টমার পাওয়া যায়নি।", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers) { item ->
                        val isOverLimit = item.balance >= item.customer.creditLimit && item.customer.creditLimit > 0
                        val agingColor = when {
                            item.daysOverdue > 60 -> Color(0xFFBA1A1A) // 🔴 Red
                            item.daysOverdue > 30 -> Color(0xFFD97706) // 🟡 Yellow/Orange
                            else -> Color(0xFF059669) // 🟢 Green
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(10.dp).clip(CircleShape).background(agingColor)
                                        )
                                        Column {
                                            Text(
                                                item.customer.nameBn,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                item.customer.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "৳${item.balance.toBn()}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.balance > 0) Color(0xFFBA1A1A) else Color(0xFF059669)
                                            )
                                        )
                                        if (item.balance > 0) {
                                            Text(
                                                "${item.daysOverdue.toBn()} দিন বাকি",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = agingColor
                                            )
                                        } else {
                                            Text("পরিশোধিত", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                                        }
                                    }
                                }

                                if (isOverLimit) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFFDAD6), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "⚠️ ক্রেডিট লিমিট (৳${item.customer.creditLimit.toBn()}) পার হয়ে গেছে!",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF410002),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { selectedCustomerForLedger = item }) {
                                        Text("হিসাব দেখুন", fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { selectedCustomerForPayment = item },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("টাকা আদায়", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomer) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var limit by remember { mutableStateOf("5000") }

        AlertDialog(
            onDismissRequest = { showAddCustomer = false },
            title = { Text("নতুন খাতা কাস্টমার যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("কাস্টমারের নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("ঠিকানা (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("ক্রেডিট লিমিট (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            viewModel.createCustomer(name, phone, address, limit.toDoubleOrNull() ?: 5000.0)
                            showAddCustomer = false
                        }
                    }
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomer = false }) { Text("বাতিল") }
            }
        )
    }

    // Payment Collection Dialog
    selectedCustomerForPayment?.let { cust ->
        var payAmount by remember { mutableStateOf(if (cust.balance > 0) cust.balance.toString() else "") }
        var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }

        AlertDialog(
            onDismissRequest = { selectedCustomerForPayment = null },
            title = { Text("${cust.customer.nameBn} - বকেয়া আদায়") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("বর্তমান বকেয়া: ৳${cust.balance.toBn()}", fontWeight = FontWeight.Bold, color = Color(0xFFBA1A1A))
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("আদায়ের পরিমাণ (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("পেমেন্টের মাধ্যম:", style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMethod == PaymentMethod.CASH,
                            onClick = { selectedMethod = PaymentMethod.CASH },
                            label = { Text("ক্যাশ") }
                        )
                        FilterChip(
                            selected = selectedMethod == PaymentMethod.BKASH,
                            onClick = { selectedMethod = PaymentMethod.BKASH },
                            label = { Text("bKash") }
                        )
                        FilterChip(
                            selected = selectedMethod == PaymentMethod.NAGAD,
                            onClick = { selectedMethod = PaymentMethod.NAGAD },
                            label = { Text("Nagad") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmount.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            viewModel.recordPayment(cust.customer.id, amt, selectedMethod)
                            selectedCustomerForPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) { Text("আদায় নিশ্চিত করুন") }
            },
            dismissButton = {
                TextButton(onClick = { selectedCustomerForPayment = null }) { Text("বাতিল") }
            }
        )
    }

    // Customer Ledger View Dialog
    selectedCustomerForLedger?.let { cust ->
        val entriesFlow = remember(cust.customer.id) { viewModel.getCustomerEntries(cust.customer.id) }
        val entries by entriesFlow.collectAsState(initial = emptyList())

        AlertDialog(
            onDismissRequest = { selectedCustomerForLedger = null },
            title = {
                Column {
                    Text("${cust.customer.nameBn} - খাতার হিসাব")
                    Text("ফোন: ${cust.customer.phone} | বকেয়া: ৳${cust.balance.toBn()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (entries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("কোনো লেনদেন পাওয়া যায়নি।", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(entries) { entry ->
                                val isCredit = entry.type == KhataEntryType.CREDIT
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp)).padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(if (isCredit) "বাকি যোগ" else "টাকা জমা", fontWeight = FontWeight.Bold, color = if (isCredit) Color(0xFFBA1A1A) else Color(0xFF059669))
                                        Text(entry.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    }
                                    Text("৳${entry.amount.toBn()}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedCustomerForLedger = null }) { Text("ঠিক আছে") }
            }
        )
    }
}
