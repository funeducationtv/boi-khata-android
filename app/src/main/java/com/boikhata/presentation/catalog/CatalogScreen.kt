package com.boikhata.presentation.catalog

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
import com.boikhata.domain.model.BookCategory
import com.boikhata.domain.model.MasterCatalogBook
import com.boikhata.domain.model.Role
import com.boikhata.presentation.SessionManager
import com.boikhata.util.BengaliUtils.toBn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    sessionManager: SessionManager,
    viewModel: CatalogViewModel = hiltViewModel(),
    masterViewModel: MasterCatalogViewModel = hiltViewModel()
) {
    val books by viewModel.books.collectAsState()
    val user by sessionManager.currentUser.collectAsState()
    val hasRemoteUpdates by masterViewModel.hasRemoteUpdates.collectAsState()
    val syncMessage by masterViewModel.syncMessage.collectAsState()

    val canEdit = user?.role == Role.OWNER || user?.role == Role.MANAGER

    var showAddDialog by remember { mutableStateOf(false) }
    var showMaster by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            masterViewModel.clearMessage()
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
                Text("বই ক্যাটালগ (স্টক)", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))

                if (canEdit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ নতুন বই")
                        }
                        OutlinedButton(
                            onClick = { showMaster = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("NCTB মাস্টার")
                                if (hasRemoteUpdates) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFBA1A1A), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("নতুন দাম", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("দোকানে কোনো বই যোগ করা হয়নি। NCTB মাস্টার থেকে সহজে ইমপোর্ট করুন।", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(books) { item ->
                        val isLowStock = item.currentStock <= item.book.lowStockThreshold
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.book.titleBn, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "বিক্রয় মূল্য: ৳${item.book.sellingPrice.toBn()} | ক্যাটাগরি: ${item.book.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "স্টক: ${item.currentStock.toBn()}টি",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isLowStock) Color(0xFFBA1A1A) else Color(0xFF1E293B)
                                    )
                                    if (isLowStock) {
                                        Text("🔴 কম স্টক", color = Color(0xFFBA1A1A), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMaster) {
        MasterCatalogDialog(masterViewModel, canEdit) { showMaster = false }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var stock by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(BookCategory.GENERAL) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন বই যুক্ত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("বইয়ের নাম") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("বিক্রি মূল্য (৳)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("প্রাথমিক স্টক (সংখ্যা)") }, modifier = Modifier.fillMaxWidth())
                    Text("ক্যাটাগরি নির্বাচন:", style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = category == BookCategory.NCTB, onClick = { category = BookCategory.NCTB }, label = { Text("NCTB") })
                        FilterChip(selected = category == BookCategory.GENERAL, onClick = { category = BookCategory.GENERAL }, label = { Text("সাধারণ") })
                        FilterChip(selected = category == BookCategory.STATIONERY, onClick = { category = BookCategory.STATIONERY }, label = { Text("স্টেশনারি") })
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
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
fun MasterCatalogDialog(viewModel: MasterCatalogViewModel, canEdit: Boolean, onDismiss: () -> Unit) {
    val books by viewModel.books.collectAsState()
    val hasRemoteUpdates by viewModel.hasRemoteUpdates.collectAsState()
    var selectedBookForImport by remember { mutableStateOf<MasterCatalogBook?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("NCTB মাস্টার ক্যাটালগ")
                if (canEdit && hasRemoteUpdates) {
                    Button(
                        onClick = { viewModel.syncRemotePrices() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("দাম আপডেট 🔄", fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                if (hasRemoteUpdates) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp)).padding(8.dp)) {
                        Text("📢 সার্ভারে নতুন দাম এসেছে। 'দাম আপডেট' বাটনে চাপ দিন।", fontSize = 12.sp, color = Color(0xFF92400E))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(books) { b ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.titleBn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("শ্রেণি: ${b.classLevel} | MRP: ৳${b.mrp.toBn()}", fontSize = 12.sp, color = Color.DarkGray)
                                }
                                if (canEdit) {
                                    Button(
                                        onClick = { selectedBookForImport = b },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("দোকানে আনুন", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )

    selectedBookForImport?.let { masterBook ->
        var purchasePrice by remember { mutableStateOf((masterBook.mrp * 0.8).toString()) }
        var initialStock by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { selectedBookForImport = null },
            title = { Text("${masterBook.titleBn} - স্টক যুক্ত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MRP (বিক্রয় মূল্য): ৳${masterBook.mrp.toBn()}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("ক্রয় মূল্য (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialStock,
                        onValueChange = { initialStock = it },
                        label = { Text("প্রাথমিক সংখ্যা (স্টক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pPrice = purchasePrice.toDoubleOrNull() ?: (masterBook.mrp * 0.8)
                        val stock = initialStock.toIntOrNull() ?: 0
                        viewModel.importBook(masterBook, pPrice, stock)
                        selectedBookForImport = null
                    }
                ) { Text("যুক্ত করুন") }
            },
            dismissButton = {
                TextButton(onClick = { selectedBookForImport = null }) { Text("বাতিল") }
            }
        )
    }
}
