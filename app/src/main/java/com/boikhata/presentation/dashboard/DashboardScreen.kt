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
import com.boikhata.R
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
