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
