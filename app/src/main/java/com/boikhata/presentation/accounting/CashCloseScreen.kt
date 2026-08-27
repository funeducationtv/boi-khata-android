package com.boikhata.presentation.accounting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.util.BengaliUtils.toBn
import com.boikhata.domain.model.CashbookEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise Cash Close Screen for daily reconciliation.
 * Allows bookstore owners to compare system cash vs physical cash.
 */
@Composable
fun CashCloseScreen(
    viewModel: CashCloseViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var physicalCash by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "দৈনিক ক্যাশ ক্লোজ",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Today's Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "আজকের সারসংক্ষেপ",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("মোট বিক্রয়:")
                    Text("৳${uiState.totalSales.toBn()}")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("মোট আদায়:")
                    Text("৳${uiState.totalCollection.toBn()}")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("মোট খরচ:")
                    Text("৳${uiState.totalExpense.toBn()}")
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "সিস্টেম ক্যাশ:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "৳${uiState.systemCash.toBn()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Physical Cash Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "প্রকৃত ক্যাশ গণনা করুন",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = physicalCash,
                    onValueChange = { physicalCash = it },
                    label = { Text("প্রকৃত ক্যাশ (টাকা)") },
                    placeholder = { Text("যেমন: ৫০০০") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Calculate and show difference
                val physicalCashValue = physicalCash.toDoubleOrNull() ?: 0.0
                val difference = physicalCashValue - uiState.systemCash
                
                if (physicalCash.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("পার্থক্য:")
                        Text(
                            text = "৳${difference.toBn()}",
                            color = if (difference > 0) MaterialTheme.colorScheme.error
                                    else if (difference < 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (difference != 0.0) {
                        Text(
                            text = if (difference > 0) "অতিরিক্ত ক্যাশ" else "ঘাটতি",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (difference > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        viewModel.submitCashClose(physicalCashValue)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = physicalCash.isNotEmpty() && uiState.systemCash > 0
                ) {
                    Text("ক্যাশ ক্লোজ সম্পন্ন করুন")
                }
            }
        }
        
        // Recent Cashbook Entries
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "সাম্প্রতিক লেনদেন",
            style = MaterialTheme.typography.titleMedium
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.recentEntries.size) { index ->
                val entry = uiState.recentEntries[index]
                CashbookEntryItem(entry)
            }
        }
    }
}

@Composable
private fun CashbookEntryItem(entry: CashbookEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = entry.description)
                Text(
                    text = formatDate(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "${if (entry.type == com.boikhata.domain.model.CashbookType.CREDIT) "+" else "-"} ৳${entry.amount.toBn()}",
                color = if (entry.type == com.boikhata.domain.model.CashbookType.CREDIT)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
