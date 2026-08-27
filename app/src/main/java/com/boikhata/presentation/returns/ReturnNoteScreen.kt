package com.boikhata.presentation.returns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.BookCondition
import com.boikhata.domain.model.ReturnReason
import com.boikhata.util.toBn
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReturnNoteScreen(
    tenantId: String,
    userId: String,
    viewModel: ReturnNoteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddBookDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বই রিটার্ন / এক্সচেঞ্জ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(error, color = Color.Red)
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("বন্ধ করুন")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Success message
            uiState.successMessage?.let { success ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(success, color = Color.Green)
                        TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                            Text("বন্ধ করুন")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Selected books list
            if (uiState.selectedBooks.isNotEmpty()) {
                Text(
                    "নির্বাচিত বই (${uiState.selectedBooks.size}টি)",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedBooks.values.toList()) { item ->
                        ReturnLineItemCard(
                            item = item,
                            onRemove = { viewModel.removeBookFromReturn(item.bookId) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total refund amount
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "মোট ফেরত:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "৳${uiState.totalRefund.toBn()}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Reason selector
            Text("রিটার্নের কারণ:", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReturnReason.values().take(4).forEach { reason ->
                    FilterChip(
                        selected = uiState.returnReason == reason,
                        onClick = { viewModel.updateReturnReason(reason) },
                        label = { Text(reason.nameBn()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Condition selector
            Text("বইয়ের অবস্থা:", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookCondition.values().forEach { condition ->
                    FilterChip(
                        selected = uiState.bookCondition == condition,
                        onClick = { viewModel.updateBookCondition(condition) },
                        label = { Text(condition.nameBn()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes field
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("অতিরিক্ত নোটস (ঐচ্ছিক)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddBookDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("বই যোগ করুন")
                }

                Button(
                    onClick = {
                        // TODO: Get bill ID and customer ID from context
                        viewModel.submitReturnNote(
                            tenantId = tenantId,
                            originalBillId = "TODO",
                            customerId = null,
                            userId = userId
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.selectedBooks.isNotEmpty() && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("জমা দিন")
                    }
                }
            }
        }
    }

    // Add book dialog
    if (showAddBookDialog) {
        AddBookToReturnDialog(
            onDismiss = { showAddBookDialog = false },
            onBookAdded = { bookId, title, qty, price ->
                viewModel.addBookToReturn(bookId, title, qty, price)
                showAddBookDialog = false
            }
        )
    }
}

@Composable
private fun ReturnLineItemCard(
    item: ReturnLineItem,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.bookTitle,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("পরিমাণ: ${item.quantity.toBn()}")
                Text("দাম: ৳${item.unitPrice.toBn()}")
                Text(
                    "মোট: ৳${item.lineTotal.toBn()}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "কারণ: ${item.reason.nameBn()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "অবস্থা: ${item.condition.nameBn()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AddBookToReturnDialog(
    onDismiss: () -> Unit,
    onBookAdded: (String, String, Int, Double) -> Unit
) {
    var bookId by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বই নির্বাচন করুন") },
        text = {
            Column {
                OutlinedTextField(
                    value = bookId,
                    onValueChange = { bookId = it },
                    label = { Text("বই ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("বইয়ের নাম") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("পরিমাণ") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("প্রতি ইউনিট দাম") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val prc = price.toDoubleOrNull() ?: 0.0
                    onBookAdded(bookId, bookTitle, qty, prc)
                },
                enabled = bookTitle.isNotBlank() && price.isNotBlank()
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

// Extension functions for Bengali names
fun ReturnReason.nameBn(): String = when (this) {
    ReturnReason.DAMAGE -> "ক্ষতিগ্রস্ত"
    ReturnReason.WRONG_BOOK -> "ভুল বই"
    ReturnReason.QUALITY_ISSUE -> "মানের সমস্যা"
    ReturnReason.DUPLICATE -> "ডুপ্লিকেট"
    ReturnReason.CUSTOMER_REQUEST -> "কাস্টমার অনুরোধ"
    ReturnReason.OTHER -> "অন্যান্য"
}

fun BookCondition.nameBn(): String = when (this) {
    BookCondition.SEALED -> "সিলমোহর করা"
    BookCondition.USED -> "ব্যবহৃত"
    BookCondition.OPENED -> "খোলা"
    BookCondition.DAMAGED -> "ক্ষতিগ্রস্ত"
}
