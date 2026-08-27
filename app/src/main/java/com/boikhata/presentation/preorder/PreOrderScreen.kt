package com.boikhata.presentation.preorder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.PreOrderStatus
import com.boikhata.presentation.theme.BoiKhataTheme as BoikhataTheme

@Composable
fun PreOrderScreen(
    viewModel: PreOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BoikhataTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("প্রি-অর্ডার ম্যানেজমেন্ট") }) }) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (uiState.orders.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("কোনো প্রি-অর্ডার নেই।")
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(uiState.orders) { order ->
                            PreOrderCard(order, onStatusChange = { newStatus ->
                                // viewModel.updateStatus(order.preOrder.id, newStatus)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreOrderCard(order: com.boikhata.domain.model.PreOrderWithDetails, onStatusChange: (PreOrderStatus) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("বই: ${order.book.titleBn}", style = MaterialTheme.typography.titleMedium)
            Text("কাস্টমার: ${order.customer?.nameBn ?: "নগদ"}")
            Text("পরিমাণ: ${order.preOrder.quantity} | এডভান্স: ৳${order.preOrder.advanceAmount}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreOrderStatus.values().forEach { status ->
                    FilterChip(
                        selected = order.preOrder.status == status,
                        onClick = { onStatusChange(status) },
                        label = { Text(status.name) }
                    )
                }
            }
        }
    }
}
