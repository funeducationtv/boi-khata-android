#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 15. REPAIR USER MANAGEMENT PIN RESET
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
import com.boikhata.domain.model.User

@Composable
fun UserManagementScreen(viewModel: UserViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var userToReset by remember { mutableStateOf<User?>(null) }

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
                        Row {
                            TextButton(onClick = { userToReset = user }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                                Text("PIN Reset")
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
                }) { Text("যোগ করুন") }
            }
        )
    }

    userToReset?.let { u ->
        var ownerPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { userToReset = null },
            title = { Text("PIN Reset for \${u.name}") },
            text = {
                Column {
                    OutlinedTextField(value = ownerPin, onValueChange = { ownerPin = it }, label = { Text("আপনার (Owner) পিন দিন") })
                    OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("নতুন পিন দিন (৪ সংখ্যা)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (ownerPin.isNotEmpty() && newPin.length == 4) {
                        viewModel.resetPin(u.id, ownerPin, newPin)
                        userToReset = null
                    }
                }) { Text("রিসেট করুন") }
            }
        )
    }
}
INNER_EOF

# 16. UPDATE USER VIEWMODEL
cat << 'INNER_EOF' > $PKG_DIR/presentation/users/UserViewModel.kt
package com.boikhata.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.Role
import com.boikhata.domain.repository.AuthRepository
import com.boikhata.domain.repository.UserRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    val sessionManager: SessionManager
) : ViewModel() {
    val users = userRepository.getUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addUser(name: String, role: Role, pin: String) {
        viewModelScope.launch { userRepository.addUser(name, role, pin) }
    }

    fun disableUser(userId: String) {
        viewModelScope.launch { userRepository.disableUser(userId) }
    }
    
    fun resetPin(userId: String, ownerPin: String, newPin: String) {
        viewModelScope.launch { userRepository.resetPin(userId, ownerPin, newPin) }
    }
    
    fun switchRole(userId: String, pin: String) {
        viewModelScope.launch { 
            authRepository.switchRole(userId, pin).onSuccess {
                sessionManager.setSession(it)
            }
        }
    }
}
INNER_EOF

# 17. UPDATE NAVIGATION TO WIRE SCREENS
cat << 'INNER_EOF' > $PKG_DIR/presentation/Navigation.kt
package com.boikhata.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.boikhata.presentation.billing.BillingScreen
import com.boikhata.presentation.catalog.CatalogScreen
import com.boikhata.presentation.dashboard.DashboardScreen
import com.boikhata.presentation.users.UserManagementScreen
import com.boikhata.presentation.users.UserViewModel
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute
@Serializable data object BillRoute
@Serializable data object KhataRoute
@Serializable data object StockRoute
@Serializable data object AccountsRoute
@Serializable data object MoreRoute

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
        BottomNavItem(R.string.stock, StockRoute, "📦", listOf(Role.OWNER, Role.MANAGER, Role.SALES, Role.ACCOUNTANT)),
        BottomNavItem(R.string.accounts, AccountsRoute, "📊", listOf(Role.OWNER, Role.ACCOUNTANT)),
        BottomNavItem(R.string.more, MoreRoute, "☰", listOf(Role.OWNER, Role.MANAGER, Role.ACCOUNTANT, Role.SALES))
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
            composable<BillRoute> { BillingScreen(sessionManager) }
            composable<KhataRoute> { PlaceholderScreen(R.string.khata) }
            composable<StockRoute> { CatalogScreen(sessionManager) }
            composable<AccountsRoute> { PlaceholderScreen(R.string.accounts) }
            composable<MoreRoute> { MoreScreen(currentUser, sessionManager) }
        }
    }
}

@Composable
fun MoreScreen(currentUser: User?, sessionManager: SessionManager, userVm: UserViewModel = hiltViewModel()) {
    var showUserMgmt by remember { mutableStateOf(false) }
    var showSwitchDialog by remember { mutableStateOf(false) }

    if (showUserMgmt) {
        UserManagementScreen()
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("আপনার প্রোফাইল: \${currentUser?.name} (\${currentUser?.role})", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (currentUser?.role == Role.OWNER) {
            Button(onClick = { showUserMgmt = true }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight=48.dp)) {
                Text("ব্যবহারকারী ব্যবস্থাপনা")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (currentUser?.role == Role.OWNER || currentUser?.role == Role.MANAGER) {
            Button(onClick = { showSwitchDialog = true }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight=48.dp)) {
                Text("ভূমিকা পরিবর্তন (Switch Role)")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = { sessionManager.lock() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight=48.dp)) {
            Text("লক করুন (Lock App)")
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
                                Text(u.name, modifier = Modifier.fillMaxWidth().clickable { selectedUser = u }.padding(16.dp), fontSize = 18.sp)
                            }
                        }
                    } else {
                        Text("পিন দিন (\${selectedUser!!.name}):")
                        OutlinedTextField(value = pin, onValueChange = { pin = it })
                    }
                }
            },
            confirmButton = {
                if (selectedUser != null) {
                    Button(onClick = { userVm.switchRole(selectedUser!!.id, pin); showSwitchDialog = false }) { Text("লগইন") }
                }
            }
        )
    }
}

@Composable
fun PlaceholderScreen(titleResId: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.coming_next_phase), style = MaterialTheme.typography.titleLarge)
    }
}
INNER_EOF

