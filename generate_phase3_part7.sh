#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 14. SCREENS
cat << 'INNER_EOF' > $PKG_DIR/presentation/catalog/CatalogScreen.kt
package com.boikhata.presentation.catalog

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
import com.boikhata.domain.model.BookCategory
import com.boikhata.domain.model.Role
import com.boikhata.presentation.SessionManager
import com.boikhata.util.BengaliUtils.toBn

@Composable
fun CatalogScreen(sessionManager: SessionManager, viewModel: CatalogViewModel = hiltViewModel()) {
    val books by viewModel.books.collectAsState()
    val user by sessionManager.currentUser.collectAsState()
    val canEdit = user?.role == Role.OWNER || user?.role == Role.MANAGER
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)).padding(16.dp)) {
        Text("বই ক্যাটালগ (স্টক)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (canEdit) {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.defaultMinSize(minHeight=48.dp)) { Text("বই যোগ করুন") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(books) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(item.book.titleBn, style = MaterialTheme.typography.titleMedium)
                            Text("৳${item.book.sellingPrice.toBn()} | ক্যাটাগরি: ${item.book.category}", color = Color.Gray)
                        }
                        Column {
                            Text("স্টক: ${item.currentStock.toBn()}", color = if (item.currentStock <= item.book.lowStockThreshold) Color.Red else Color.Black)
                            if (item.currentStock <= item.book.lowStockThreshold) Text("🔴 লো স্টক", color = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var stock by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(BookCategory.GENERAL) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন বই") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("বইয়ের নাম") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("বিক্রি মূল্য (৳)") })
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("প্রাথমিক স্টক") })
                    Row {
                        Button(onClick={category=BookCategory.NCTB}, colors=ButtonDefaults.buttonColors(containerColor=if(category==BookCategory.NCTB) Color.Blue else Color.Gray)) { Text("NCTB") }
                        Button(onClick={category=BookCategory.GENERAL}, colors=ButtonDefaults.buttonColors(containerColor=if(category==BookCategory.GENERAL) Color.Blue else Color.Gray)) { Text("Gen") }
                        Button(onClick={category=BookCategory.STATIONERY}, colors=ButtonDefaults.buttonColors(containerColor=if(category==BookCategory.STATIONERY) Color.Blue else Color.Gray)) { Text("Stat") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && price.toDoubleOrNull() != null) {
                        viewModel.addBook(title, category, price.toDouble(), stock.toIntOrNull() ?: 0)
                        showAddDialog = false
                    }
                }) { Text("যোগ করুন") }
            }
        )
    }
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/presentation/billing/BillingScreen.kt
package com.boikhata.presentation.billing

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
import com.boikhata.domain.model.PaymentMethod
import com.boikhata.domain.model.Role
import com.boikhata.presentation.SessionManager
import com.boikhata.util.BengaliUtils.toBn

@Composable
fun BillingScreen(sessionManager: SessionManager, viewModel: BillingViewModel = hiltViewModel()) {
    val catalog by viewModel.catalog.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val user by sessionManager.currentUser.collectAsState()
    val canVoid = user?.role == Role.OWNER || user?.role == Role.MANAGER
    val canBill = user?.role != Role.ACCOUNTANT
    
    var viewMode by remember { mutableStateOf("NEW") } // NEW, HISTORY, QUICK
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF)).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canBill) {
                Button(onClick = { viewMode = "NEW" }) { Text("নতুন বিল") }
                if (user?.role == Role.SALES) {
                    Button(onClick = { viewMode = "QUICK" }) { Text("কুইক বিল") }
                }
            }
            Button(onClick = { viewMode = "HISTORY" }) { Text("বিল হিস্ট্রি") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (viewMode == "NEW") {
            Row(modifier=Modifier.weight(1f)) {
                // Catalog List
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(catalog) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(item.book.titleBn)
                                Text("৳${item.book.sellingPrice.toBn()}")
                                Button(onClick = { viewModel.addToCart(item, 1) }) { Text("+ কার্টে যোগ") }
                            }
                        }
                    }
                }
                // Cart
                Column(modifier = Modifier.weight(1f).padding(start=8.dp)) {
                    Text("কার্ট (${cart.size})", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(cart) { line ->
                            Text("${line.bookTitleBn} - ৳${line.lineTotal.toBn()}")
                        }
                    }
                    if (cart.isNotEmpty()) {
                        val sub = cart.sumOf { it.lineTotal }
                        Text("মোট: ৳${sub.toBn()}", style=MaterialTheme.typography.titleMedium)
                        var paid by remember { mutableStateOf(sub.toString()) }
                        OutlinedTextField(value = paid, onValueChange = { paid = it }, label = { Text("পেমেন্ট") })
                        Button(onClick = { viewModel.checkout("ওয়াক-ইন", "", paid.toDoubleOrNull()?:sub, PaymentMethod.CASH) }) { Text("কনফার্ম বিল") }
                    }
                }
            }
        } else if (viewMode == "HISTORY") {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(bills) { bill ->
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(bill.billNumber)
                                Text("৳${bill.totalAmount.toBn()} - ${bill.syncStatus}", color = Color.Gray)
                            }
                            if (canVoid && bill.syncStatus != com.boikhata.domain.model.SyncStatus.CONFLICT) {
                                Button(onClick = { viewModel.voidBill(bill.id) }, colors = ButtonDefaults.buttonColors(containerColor=Color.Red)) { Text("Void") }
                            }
                        }
                    }
                }
            }
        } else if (viewMode == "QUICK") {
            Text("কুইক বিল (Sales)", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(catalog) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.book.titleBn} ৳${item.book.sellingPrice.toBn()}")
                            Button(onClick = { 
                                viewModel.addToCart(item, 1)
                                viewModel.checkout("ওয়াক-ইন", "", item.book.sellingPrice, PaymentMethod.CASH) 
                            }) { Text("কুইক পে") }
                        }
                    }
                }
            }
        }
    }
}
INNER_EOF

