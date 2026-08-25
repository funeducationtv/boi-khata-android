#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# USER MANAGEMENT SCREEN
cat << 'INNER_EOF' > $PKG_DIR/presentation/users/UserManagementScreen.kt
package com.boikhata.presentation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.Role

@Composable
fun UserManagementScreen(viewModel: UserViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)).padding(16.dp)) {
        Text("ব্যবহারকারী ব্যবস্থাপনা", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAddDialog = true }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
            Text("নতুন ব্যবহারকারী যোগ করুন")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(users) { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(user.name, style = MaterialTheme.typography.titleMedium)
                            Text(user.role.name, color = Color.Gray)
                        }
                        if (user.role != Role.OWNER && user.isActive) {
                            TextButton(onClick = { viewModel.disableUser(user.id) }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                                Text("বন্ধ করুন", color = Color.Red)
                            }
                        } else if (!user.isActive) {
                            Text("বন্ধ", color = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var pin by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(Role.SALES) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন ব্যবহারকারী") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম") })
                    OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("পিন (৪ সংখ্যা)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { role = Role.MANAGER }, colors = ButtonDefaults.buttonColors(containerColor = if(role==Role.MANAGER) Color.Blue else Color.Gray)) { Text("Manager") }
                        Button(onClick = { role = Role.SALES }, colors = ButtonDefaults.buttonColors(containerColor = if(role==Role.SALES) Color.Blue else Color.Gray)) { Text("Sales") }
                        Button(onClick = { role = Role.ACCOUNTANT }, colors = ButtonDefaults.buttonColors(containerColor = if(role==Role.ACCOUNTANT) Color.Blue else Color.Gray)) { Text("Acct") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && pin.length == 4) {
                        viewModel.addUser(name, role, pin)
                        showAddDialog = false
                    }
                }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("যোগ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("বাতিল") }
            }
        )
    }
}
INNER_EOF

# DASHBOARD SCREEN UPDATE (Data Layer Filtering Enforced, Role Views)
cat << 'INNER_EOF' > $PKG_DIR/presentation/dashboard/DashboardScreen.kt
package com.boikhata.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is DashboardUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0061A4))
            }
        }
        is DashboardUiState.Success -> {
            state.data?.let { DashboardContent(data = it) }
        }
    }
}

@Composable
private fun DashboardContent(data: DashboardData) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF))) {
        // Header
        val tenantName = when(data) {
            is DashboardData.Owner -> data.tenantName
            is DashboardData.Manager -> data.tenantName
            is DashboardData.Sales -> data.tenantName
            is DashboardData.Accountant -> data.tenantName
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().shadow(1.dp).background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = tenantName.removeSuffix(" ▾"), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
                Text(text = "▾", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("🔄 ", fontSize = 10.sp); Text("সিঙ্ক ✓", fontSize = 10.sp, color = Color(0xFF059669)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("☁️ ", fontSize = 10.sp); Text("ব্যাকআপ ✓", fontSize = 10.sp, color = Color(0xFF059669)) }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            when (data) {
                is DashboardData.Owner -> OwnerDashboard(data)
                is DashboardData.Manager -> ManagerDashboard(data)
                is DashboardData.Sales -> SalesDashboard(data)
                is DashboardData.Accountant -> AccountantDashboard(data)
            }
        }
    }
}

@Composable
private fun OwnerDashboard(data: DashboardData.Owner) {
    if (data.licenseState == LicenseState.GRACE || data.licenseDaysRemaining <= 14) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth().background(Color(0xFFFFDAD6), RoundedCornerShape(16.dp)).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("⚠ সাবস্ক্রিপশন ${data.licenseDaysRemaining} দিন বাকি", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF410002))
                Box(modifier = Modifier.background(Color(0xFFBA1A1A), RoundedCornerShape(8.dp)).clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("এখনই পরিশোধ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem(modifier = Modifier.weight(1f), title = "আজ বিক্রি", value = data.todaySales, valueColor = Color(0xFF0061A4))
        StatItem(modifier = Modifier.weight(1f), title = "আজ আদায়", value = data.todayCollection, valueColor = Color(0xFF047857))
        StatItem(modifier = Modifier.weight(1f), title = "আজ লাভ", value = data.todayProfit, valueColor = Color(0xFFC2410C))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(modifier = Modifier.weight(1f), text = "নতুন বিল", icon = "📄")
        ActionButton(modifier = Modifier.weight(1f), text = "বাকি আদায়", icon = "💰")
        ActionButton(modifier = Modifier.weight(1f), text = "খরচ", icon = "📉")
        ActionButton(modifier = Modifier.weight(1f), text = "রিপোর্ট", icon = "📊")
    }
    Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("আজ কার কাছে আদায় করবেন?", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("সব দেখুন", fontSize = 12.sp, color = Color(0xFF2563EB))
        }
        data.collectTodayList.forEach { CollectTodayItemRow(it) }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(16.dp)).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📦 কম স্টক: ${data.lowStockCount}টি", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                Text("অর্ডার করুন →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
            }
        }
    }
}

@Composable
private fun ManagerDashboard(data: DashboardData.Manager) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem(modifier = Modifier.weight(1f), title = "আজ বিক্রি", value = data.todaySales, valueColor = Color(0xFF0061A4))
        StatItem(modifier = Modifier.weight(1f), title = "আজ লাভ", value = data.todayProfit, valueColor = Color(0xFFC2410C))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(modifier = Modifier.weight(1f), text = "নতুন বিল", icon = "📄")
        ActionButton(modifier = Modifier.weight(1f), text = "স্টক", icon = "📦")
        ActionButton(modifier = Modifier.weight(1f), text = "রিপোর্ট", icon = "📊")
    }
    Box(modifier = Modifier.padding(16.dp).fillMaxWidth().background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("📦 কম স্টক: ${data.lowStockCount}টি", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
            Text("অর্ডার করুন →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
        }
    }
}

@Composable
private fun SalesDashboard(data: DashboardData.Sales) {
    ActionButton(modifier = Modifier.padding(16.dp).fillMaxWidth().height(100.dp), text = "নতুন বিল", icon = "📄")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem(modifier = Modifier.weight(1f), title = "আপনার শিফট বিক্রি", value = data.shiftSales, valueColor = Color(0xFF0061A4))
        StatItem(modifier = Modifier.weight(1f), title = "ক্যাশ ড্রয়ার", value = data.cashDrawer, valueColor = Color(0xFF047857))
    }
}

@Composable
private fun AccountantDashboard(data: DashboardData.Accountant) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem(modifier = Modifier.weight(1f), title = "আজকের খরচ", value = data.expenseSummary, valueColor = Color(0xFFC2410C))
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("ক্যাশ খাতা ব্যালেন্স", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        StatItem(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), title = "নগদ", value = data.cashbookNogad, valueColor = Color.Black)
        StatItem(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), title = "বিকাশ", value = data.cashbookBkash, valueColor = Color(0xFFE2136E))
        StatItem(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), title = "ব্যাংক", value = data.cashbookBank, valueColor = Color(0xFF0061A4))
    }
}

@Composable
private fun StatItem(modifier: Modifier = Modifier, title: String, value: Int, valueColor: Color) {
    Column(modifier = modifier.shadow(1.dp, RoundedCornerShape(16.dp)).background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280), modifier = Modifier.padding(bottom = 4.dp))
        Text("৳$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun ActionButton(modifier: Modifier = Modifier, text: String, icon: String) {
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFD1E4FF)).clickable {  }.defaultMinSize(minHeight = 48.dp).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.padding(bottom = 4.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
    }
}

@Composable
private fun CollectTodayItemRow(item: CollectItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                if (item.isCritical) Text("🔴", fontSize = 10.sp)
            }
            Text("৳${item.amount} (${item.daysOverdue} দিন)", fontSize = 13.sp, color = Color(0xFF6B7280))
        }
        Box(modifier = Modifier.background(Color(0xFF0061A4), RoundedCornerShape(12.dp)).clickable { }.defaultMinSize(minHeight = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text("আদায়", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
INNER_EOF
