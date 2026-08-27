package com.boikhata.presentation.bundles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.BundleWithItems
import com.boikhata.presentation.theme.BoikhataTheme

@Composable
fun BundlesScreen(
    viewModel: BundlesViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    BoikhataTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("সিজোনাল বান্ডল অফার") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = onNavigateToCreate) {
                    Text("নতুন বান্ডল")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (uiState.bundles.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("কোনো বান্ডল অফার নেই। নতুন তৈরি করুন।")
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(uiState.bundles) { bundle ->
                            BundleCard(bundle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BundleCard(bundle: BundleWithItems) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = bundle.bundle.nameBn, style = MaterialTheme.typography.titleMedium)
            Text(text = "মূল্য: ৳${bundle.bundle.bundlePrice} (সঞ্চয়: ৳${bundle.bundle.originalTotalPrice - bundle.bundle.bundlePrice})")
            Text(text = "মেয়াদ: ${android.icu.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(bundle.bundle.validTo))}")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            bundle.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.book.titleBn} x ${item.bundleItem.quantity}")
                    if (item.bundleItem.isFreeItem) {
                        Text("ফ্রি!", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
