#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

cat << 'INNER_EOF' > $PKG_DIR/presentation/dashboard/DashboardScreen.kt
package com.boikhata.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DashboardUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DashboardUiState.Success -> {
            DashboardContent(data = state.data)
        }
    }
}

@Composable
private fun DashboardContent(data: DashboardData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = data.tenantName, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.sync_status), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.backup_status), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Banner
        if (data.licenseState == LicenseState.GRACE || data.licenseDaysRemaining <= 14) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.subscription_alert, data.licenseDaysRemaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD32F2F)
                    )
                    TextButton(onClick = { /* Pay */ }) {
                        Text(text = stringResource(R.string.pay_now), color = Color(0xFFD32F2F))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(title = stringResource(R.string.today_sales), value = data.todaySales)
            StatItem(title = stringResource(R.string.today_collection), value = data.todayCollection)
            StatItem(title = stringResource(R.string.today_profit), value = data.todayProfit)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionButton(text = stringResource(R.string.new_bill))
            ActionButton(text = stringResource(R.string.collect_dues))
            ActionButton(text = stringResource(R.string.add_expense))
            ActionButton(text = stringResource(R.string.reports))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Collect Today Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.collect_today_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.see_all), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                data.collectTodayList.forEach { item ->
                    CollectTodayItemRow(item)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Low Stock Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.low_stock, data.lowStockCount),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { /* Order */ }) {
                    Text(text = stringResource(R.string.order_now))
                }
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            text = "${stringResource(R.string.currency_symbol)}$value",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ActionButton(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(4.dp)
    )
}

@Composable
private fun CollectTodayItemRow(item: CollectItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "• ${item.name} — ")
            Text(
                text = "${stringResource(R.string.currency_symbol)}${item.amount}",
                fontWeight = FontWeight.Bold
            )
            Text(text = " ${stringResource(R.string.days_format, item.daysOverdue)}")
            if (item.isCritical) {
                Text(text = " 🔴")
            }
        }
        TextButton(onClick = { /* Collect */ }) {
            Text(text = stringResource(R.string.collect_button))
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/presentation/Navigation.kt
package com.boikhata.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.boikhata.presentation.dashboard.DashboardScreen
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute
@Serializable data object BillRoute
@Serializable data object KhataRoute
@Serializable data object StockRoute
@Serializable data object AccountsRoute
@Serializable data object MoreRoute

data class BottomNavItem(val titleResId: Int, val route: Any)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    val items = listOf(
        BottomNavItem(R.string.dashboard, DashboardRoute),
        BottomNavItem(R.string.bill, BillRoute),
        BottomNavItem(R.string.khata, KhataRoute),
        BottomNavItem(R.string.stock, StockRoute),
        BottomNavItem(R.string.accounts, AccountsRoute),
        BottomNavItem(R.string.more, MoreRoute)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route?.contains(item.route::class.simpleName ?: "") == true 
                    } == true
                    NavigationBarItem(
                        icon = { },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<DashboardRoute> { DashboardScreen() }
            composable<BillRoute> { PlaceholderScreen(R.string.bill) }
            composable<KhataRoute> { PlaceholderScreen(R.string.khata) }
            composable<StockRoute> { PlaceholderScreen(R.string.stock) }
            composable<AccountsRoute> { PlaceholderScreen(R.string.accounts) }
            composable<MoreRoute> { PlaceholderScreen(R.string.more) }
        }
    }
}

@Composable
fun PlaceholderScreen(titleResId: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.coming_next_phase),
            style = MaterialTheme.typography.titleLarge
        )
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/MainActivity.kt
package com.boikhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.boikhata.presentation.MainApp
import com.boikhata.presentation.theme.BoiKhataTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoiKhataTheme {
                MainApp()
            }
        }
    }
}
INNER_EOF

chmod +x generate_ui.sh
./generate_ui.sh
