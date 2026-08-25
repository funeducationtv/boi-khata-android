package com.boikhata.presentation.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.CashbookAccount
import com.boikhata.domain.model.CashbookEntryType
import com.boikhata.domain.model.Role
import com.boikhata.presentation.SessionManager
import com.boikhata.util.BengaliUtils.toBn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(sessionManager: SessionManager, viewModel: AccountingViewModel = hiltViewModel()) {
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val pnl by viewModel.pnl.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val cashbookEntries by viewModel.cashbookEntries.collectAsState()

    val user by sessionManager.currentUser.collectAsState()
    if (user?.role == Role.SALES) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)), contentAlignment = Alignment.Center) {
            Text("হিসাব দেখার অনুমতি কেবলমাত্র মালিক ও হিসাবরক্ষকের রয়েছে।", color = Color.Gray)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Expenses, 1: Cashbook, 2: P&L
    var showAddExpense by remember { mutableStateOf(false) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)).padding(16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("হিসাব নিকাশ", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
            if (selectedTab == 0) {
                Button(
                    onClick = { showAddExpense = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ নতুন খরচ")
                }
            } else if (selectedTab == 1) {
                OutlinedButton(
                    onClick = { showOpeningBalanceDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ জমা/ওপেনিং")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("খরচের তালিকা") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("ক্যাশ বুক (৩-অ্যাকাউন্ট)") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("লাভ-ক্ষতি") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Expenses View
                if (expenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("এখনও কোনো খরচ যুক্ত করা হয়নি।", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(expenses) { exp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(exp.category.nameBn, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        Text(exp.expense.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Text("৳${exp.expense.amount.toBn()}", fontWeight = FontWeight.Bold, color = Color(0xFFBA1A1A), fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Cashbook View (Cash, bKash, Bank)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedAccount == CashbookAccount.CASH,
                        onClick = { viewModel.selectAccount(CashbookAccount.CASH) },
                        label = { Text("💵 ক্যাশ (নগদ)") }
                    )
                    FilterChip(
                        selected = selectedAccount == CashbookAccount.BKASH,
                        onClick = { viewModel.selectAccount(CashbookAccount.BKASH) },
                        label = { Text("📱 বিকাশ") }
                    )
                    FilterChip(
                        selected = selectedAccount == CashbookAccount.BANK,
                        onClick = { viewModel.selectAccount(CashbookAccount.BANK) },
                        label = { Text("🏦 ব্যাংক") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (cashbookEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("${selectedAccount.name} অ্যাকাউন্টে কোনো লেনদেন নেই।", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cashbookEntries) { cb ->
                            val isIncome = cb.type == CashbookEntryType.INCOME
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cb.description, fontWeight = FontWeight.SemiBold)
                                        Text(if (isIncome) "জমা (Income)" else "খরচ (Expense)", style = MaterialTheme.typography.bodySmall, color = if (isIncome) Color(0xFF059669) else Color(0xFFBA1A1A))
                                    }
                                    Text(
                                        "${if (isIncome) "+" else "-"}৳${cb.amount.toBn()}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncome) Color(0xFF059669) else Color(0xFFBA1A1A),
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // P&L View
                Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("মাসিক লাভ-ক্ষতি বিবরণী (P&L)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট বিক্রয় রাজস্ব:")
                                Text("৳${pnl.totalRevenue.toBn()}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মালিকের উত্তোলন (Drawings):")
                                Text("৳${pnl.totalDrawings.toBn()}", color = Color.DarkGray)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট পরিচালন ব্যয় (Expenses):")
                                Text("৳${pnl.totalExpenses.toBn()}", color = Color(0xFFBA1A1A))
                            }
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("নিট লাভ / ক্ষতি:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    "৳${pnl.profit.toBn()}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (pnl.profit >= 0) Color(0xFF059669) else Color(0xFFBA1A1A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpense) {
        var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
        var amount by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExpense = false },
            title = { Text("নতুন খরচ যুক্ত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("খরচের খাত (ক্যাটাগরি):", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (selectedCatId == cat.id) Color(0xFFE0E7FF) else Color.Transparent, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedCatId == cat.id, onClick = { selectedCatId = cat.id })
                                Text(cat.nameBn, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("টাকার পরিমাণ (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("বিবরণ (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && amt > 0 && selectedCatId.isNotBlank()) {
                            viewModel.addExpense(selectedCatId, amt, desc.ifBlank { "নিয়মিত খরচ" })
                            showAddExpense = false
                        }
                    }
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpense = false }) { Text("বাতিল") }
            }
        )
    }

    // Opening Balance / Deposit Dialog
    if (showOpeningBalanceDialog) {
        var depositAccount by remember { mutableStateOf(selectedAccount) }
        var depositAmount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showOpeningBalanceDialog = false },
            title = { Text("প্রারম্ভিক জমা / ব্যালেন্স যুক্ত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("অ্যাকাউন্ট: ${depositAccount.name}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("জমার পরিমাণ (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depositAmount.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            viewModel.addOpeningBalance(depositAccount, amt)
                            showOpeningBalanceDialog = false
                        }
                    }
                ) { Text("জমা করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showOpeningBalanceDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
