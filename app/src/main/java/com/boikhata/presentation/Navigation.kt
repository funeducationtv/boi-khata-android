package com.boikhata.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.boikhata.domain.model.Role
import com.boikhata.domain.model.User
import com.boikhata.presentation.auth.AuthViewModel
import com.boikhata.presentation.auth.LockScreen
import com.boikhata.presentation.auth.OnboardingScreen
import com.boikhata.presentation.cloud.CloudAccountScreen
import com.boikhata.presentation.dashboard.DashboardScreen
import com.boikhata.presentation.subscription.SubscriptionScreen
import com.boikhata.presentation.users.UserManagementScreen
import com.boikhata.presentation.users.UserViewModel
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute
@Serializable data object BillRoute
@Serializable data object KhataRoute
@Serializable data object StockRoute
@Serializable data object AccountsRoute
@Serializable data object MoreRoute
@Serializable data object CloudAccountRoute
@Serializable data object SubscriptionRoute
@Serializable data object UserManagementRoute

data class BottomNavItem(val titleResId: Int, val route: Any, val icon: String, val allowedRoles: List<Role>)

@Composable
fun RootApp(
    sessionManager: SessionManager,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val hasOwner by authViewModel.hasOwner.collectAsState()
    val isLocked by sessionManager.isLocked.collectAsState()

    if (!hasOwner) {
        OnboardingScreen(authViewModel)
    } else if (isLocked) {
        LockScreen(authViewModel)
    } else {
        MainApp(sessionManager)
    }
}

@Composable
fun MainApp(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val currentUser by sessionManager.currentUser.collectAsState()
    val role = currentUser?.role ?: Role.SALES

    val allItems = listOf(
        BottomNavItem(R.string.dashboard, DashboardRoute, "🏠", listOf(Role.OWNER, Role.MANAGER, Role.SALES, Role.ACCOUNTANT)),
        BottomNavItem(R.string.bill, BillRoute, "🧾", listOf(Role.OWNER, Role.MANAGER, Role.SALES)),
        BottomNavItem(R.string.khata, KhataRoute, "📔", listOf(Role.OWNER)),
        BottomNavItem(R.string.stock, StockRoute, "📦", listOf(Role.OWNER, Role.MANAGER, Role.SALES)),
        BottomNavItem(R.string.accounts, AccountsRoute, "📊", listOf(Role.OWNER, Role.ACCOUNTANT)),
        BottomNavItem(R.string.more, MoreRoute, "☰", listOf(Role.OWNER, Role.MANAGER, Role.ACCOUNTANT))
    )
    val allowedItems = allItems.filter { it.allowedRoles.contains(role) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, modifier = Modifier.border(1.dp, Color(0xFFF3F4F6))) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                allowedItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route?.contains(item.route::class.simpleName ?: "") == true } == true
                    NavigationBarItem(
                        icon = { Text(item.icon, fontSize = 20.sp, modifier = Modifier.padding(bottom = 2.dp)) },
                        label = { Text(stringResource(item.titleResId), fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0061A4), selectedTextColor = Color(0xFF0061A4),
                            unselectedIconColor = Color(0xFF9CA3AF), unselectedTextColor = Color(0xFF9CA3AF),
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = DashboardRoute, modifier = Modifier.padding(innerPadding).background(Color(0xFFFDFBFF))) {
            composable<DashboardRoute> { DashboardScreen() }
            composable<BillRoute> { com.boikhata.presentation.billing.BillingScreen(sessionManager) }
            composable<KhataRoute> { com.boikhata.presentation.khata.KhataScreen(sessionManager) }
            composable<StockRoute> { com.boikhata.presentation.catalog.CatalogScreen(sessionManager) }
            composable<AccountsRoute> { com.boikhata.presentation.accounting.AccountingScreen(sessionManager) }
            composable<MoreRoute> {
                MoreScreen(
                    currentUser = currentUser,
                    sessionManager = sessionManager,
                    onNavigateToCloud = { navController.navigate(CloudAccountRoute) },
                    onNavigateToSubscription = { navController.navigate(SubscriptionRoute) },
                    onNavigateToUsers = { navController.navigate(UserManagementRoute) }
                )
            }
            composable<CloudAccountRoute> {
                CloudAccountScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable<SubscriptionRoute> {
                SubscriptionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable<UserManagementRoute> {
                UserManagementScreen()
            }
        }
    }
}

@Composable
fun MoreScreen(
    currentUser: User?,
    sessionManager: SessionManager,
    onNavigateToCloud: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToUsers: () -> Unit,
    userVm: UserViewModel = hiltViewModel()
) {
    var showSwitchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(currentUser?.name ?: "ইউজার", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("ভূমিকা: ${currentUser?.role?.name ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0061A4))
            }
        }

        Text("সেটিংস ও নিয়ন্ত্রণ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1E293B))

        if (currentUser?.role == Role.OWNER) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToCloud() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("☁️ ক্লাউড ব্যাকআপ ও অ্যাকাউন্ট", fontWeight = FontWeight.Bold)
                        Text("ফোন হারালেও ডাটা নিরাপদে ফিরিয়ে আনুন", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("›", fontSize = 24.sp, color = Color.Gray)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToSubscription() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("💳 সাবস্ক্রিপশন ও লাইসেন্স", fontWeight = FontWeight.Bold)
                        Text("Lite প্ল্যান (৳২৫০/মাস) ও মেয়াদ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("›", fontSize = 24.sp, color = Color.Gray)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToUsers() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("👥 কর্মচারী ও ইউজার ব্যবস্থাপনা", fontWeight = FontWeight.Bold)
                        Text("ম্যানেজার, সেলস ও হিসাবরক্ষক যোগ করুন", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("›", fontSize = 24.sp, color = Color.Gray)
                }
            }
        }

        if (currentUser?.role == Role.OWNER || currentUser?.role == Role.MANAGER) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showSwitchDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🔄 ভূমিকা পরিবর্তন (Switch User)", fontWeight = FontWeight.Bold)
                        Text("অন্য ব্যবহারকারীর পিন দিয়ে লগইন করুন", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("›", fontSize = 24.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { sessionManager.lock() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
        ) {
            Text("🔒 অ্যাপ লক করুন (Lock App)")
        }
    }

    if (showSwitchDialog) {
        val users by userVm.users.collectAsState()
        var selectedUser by remember { mutableStateOf<User?>(null) }
        var pin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            title = { Text("ব্যবহারকারী নির্বাচন করুন") },
            text = {
                Column {
                    if (selectedUser == null) {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(users) { u ->
                                Text(
                                    "${u.name} (${u.role})",
                                    modifier = Modifier.fillMaxWidth().clickable { selectedUser = u }.padding(14.dp),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        Text("পিন দিন (${selectedUser!!.name}):")
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (selectedUser != null) {
                    Button(onClick = {
                        userVm.switchRole(selectedUser!!.id, pin)
                        showSwitchDialog = false
                    }) { Text("লগইন") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
