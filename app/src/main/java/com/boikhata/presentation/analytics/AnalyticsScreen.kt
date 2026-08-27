package com.boikhata.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.AnalyticsPeriod
import com.boikhata.domain.model.ReorderSuggestion
import com.boikhata.domain.model.toBn

/**
 * Analytics Dashboard Screen
 * Displays business intelligence metrics, charts, and insights
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ব্যবসায়িক বিশ্লেষণ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        androidx.compose.material.icons.Icons.Default.arrowBack
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.error!!, color = Color.Red)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period Filter
                item {
                    PeriodFilterRow(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.setPeriod(it) }
                    )
                }

                // Key Metrics Cards
                if (uiState.analyticsData != null) {
                    item {
                        KeyMetricsGrid(analyticsData = uiState.analyticsData!!)
                    }

                    // Profit/Loss Summary
                    item {
                        ProfitLossCard(
                            totalSales = uiState.analyticsData!!.totalSales,
                            totalExpenses = uiState.analyticsData!!.totalExpenses,
                            netProfit = uiState.analyticsData!!.netProfit,
                            profitMargin = uiState.analyticsData!!.profitMargin
                        )
                    }

                    // Top Products
                    if (uiState.analyticsData!!.topProducts.isNotEmpty()) {
                        item {
                            TopProductsCard(products = uiState.analyticsData!!.topProducts)
                        }
                    }
                }

                // Inventory Alerts
                if (uiState.inventoryAnalytics != null && 
                    (uiState.inventoryAnalytics!!.lowStockItems > 0 || 
                     uiState.inventoryAnalytics!!.outOfStockItems > 0)) {
                    item {
                        InventoryAlertsCard(
                            analytics = uiState.inventoryAnalytics!!,
                            onViewSuggestions = { viewModel.showReorderSuggestions() }
                        )
                    }
                }

                // Customer Analytics
                if (uiState.customerAnalytics != null) {
                    item {
                        CustomerAnalyticsCard(analytics = uiState.customerAnalytics!!)
                    }
                }

                // Reorder Suggestions Button
                if (uiState.reorderSuggestions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.showReorderSuggestions() }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "পণ্য পুনরায় অর্ডার করুন",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${uiState.reorderSuggestions.size}টি পণ্যের পরামর্শ",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                // Export Button
                item {
                    Button(
                        onClick = { viewModel.exportReport() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("রিপোর্ট এক্সপোর্ট করুন")
                    }
                }

                if (uiState.exportSuccess) {
                    item {
                        Snackbar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("রিপোর্ট সফলভাবে এক্সপোর্ট হয়েছে")
                        }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(3000)
                            viewModel.clearExportSuccess()
                        }
                    }
                }
            }
        }
    }

    // Reorder Suggestions Dialog
    if (uiState.showReorderDialog) {
        ReorderSuggestionsDialog(
            suggestions = uiState.reorderSuggestions,
            onDismiss = { viewModel.hideReorderSuggestions() }
        )
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AnalyticsPeriod.values().take(6).forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = if (index == 0) 12.dp else 0.dp,
                    topEnd = if (index == 5) 12.dp else 0.dp,
                    bottomStart = if (index == 0) 12.dp else 0.dp,
                    bottomEnd = if (index == 5) 12.dp else 0.dp
                )
            ) {
                Text(
                    text = getPeriodDisplayName(period),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun KeyMetricsGrid(analyticsData: com.boikhata.domain.model.AnalyticsData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "মোট বিক্রয়",
            value = "৳${analyticsData.totalSales.toBn()}",
            icon = Icons.Default.ShoppingCart,
            trend = null
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "লাভ",
            value = "৳${analyticsData.netProfit.toBn()}",
            icon = Icons.Default.TrendingUp,
            trend = if (analyticsData.netProfit > 0) "positive" else "negative"
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "কাস্টমার",
            value = analyticsData.customerCount.toBn(),
            icon = Icons.Default.ShoppingCart,
            trend = null
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "বকেয়া",
            value = "৳${analyticsData.dueAmount.toBn()}",
            icon = Icons.Default.Warning,
            trend = if (analyticsData.dueAmount > 0) "warning" else null
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trend: String?
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (trend) {
                "positive" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                "negative" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                "warning" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (trend) {
                    "positive" -> MaterialTheme.colorScheme.primary
                    "negative" -> MaterialTheme.colorScheme.error
                    "warning" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfitLossCard(
    totalSales: Double,
    totalExpenses: Double,
    netProfit: Double,
    profitMargin: Double
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "লাভ-ক্ষতি সারাংশ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfitLossRow(label = "মোট বিক্রয়", value = totalSales, isPositive = true)
            Spacer(modifier = Modifier.height(8.dp))
            ProfitLossRow(label = "মোট খরচ", value = totalExpenses, isPositive = false)
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            ProfitLossRow(
                label = "নিট লাভ",
                value = netProfit,
                isPositive = netProfit >= 0,
                isBold = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    color = if (profitMargin >= 0) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "লাভের হার: ${profitMargin.toBn()}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (profitMargin >= 0) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitLossRow(
    label: String,
    value: Double,
    isPositive: Boolean,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "৳${value.toBn()}",
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isBold && isPositive -> MaterialTheme.colorScheme.primary
                isBold && !isPositive -> MaterialTheme.colorScheme.error
                isPositive -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun TopProductsCard(
    products: List<com.boikhata.domain.model.TopProduct>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "সেরা বিক্রিত পণ্য",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            products.take(5).forEachIndexed { index, product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        Text(
                            text = "${index + 1}. ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = product.titleBn,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "${product.quantitySold.toBn()} পিস",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < 4) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun InventoryAlertsCard(
    analytics: com.boikhata.domain.model.InventoryAnalytics,
    onViewSuggestions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewSuggestions() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "ইনভেন্টরি সতর্কতা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        if (analytics.outOfStockItems > 0) append("শেষ: ${analytics.outOfStockItems.toBn()}টি | ")
                        if (analytics.lowStockItems > 0) append("কম স্টক: ${analytics.lowStockItems.toBn()}টি")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CustomerAnalyticsCard(
    analytics: com.boikhata.domain.model.CustomerAnalytics
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "কাস্টমার বিশ্লেষণ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("মোট কাস্টমার", style = MaterialTheme.typography.bodySmall)
                    Text(analytics.totalCustomers.toBn(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("বকেয়া কাস্টমার", style = MaterialTheme.typography.bodySmall)
                    Text(analytics.customersWithDue.toBn(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("মোট বকেয়া", style = MaterialTheme.typography.bodySmall)
                    Text("৳${analytics.totalDueAmount.toBn()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReorderSuggestionsDialog(
    suggestions: List<ReorderSuggestion>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পণ্য পুনরায় অর্ডার করুন") },
        text = {
            LazyColumn {
                items(suggestions) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.titleBn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "বর্তমান: ${suggestion.currentStock.toBn()} | প্রস্তাবিত: ${suggestion.suggestedQuantity.toBn()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            color = when (suggestion.priority) {
                                com.boikhata.domain.model.PriorityLevel.CRITICAL -> MaterialTheme.colorScheme.error
                                com.boikhata.domain.model.PriorityLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
                                com.boikhata.domain.model.PriorityLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                                com.boikhata.domain.model.PriorityLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = suggestion.priority.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (suggestion.priority) {
                                    com.boikhata.domain.model.PriorityLevel.CRITICAL,
                                    com.boikhata.domain.model.PriorityLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("বুঝেছি")
            }
        }
    )
}

private fun getPeriodDisplayName(period: AnalyticsPeriod): String {
    return when (period) {
        AnalyticsPeriod.TODAY -> "আজ"
        AnalyticsPeriod.YESTERDAY -> "গতকাল"
        AnalyticsPeriod.THIS_WEEK -> "এই সপ্তাহ"
        AnalyticsPeriod.LAST_WEEK -> "গত সপ্তাহ"
        AnalyticsPeriod.THIS_MONTH -> "এই মাস"
        AnalyticsPeriod.LAST_MONTH -> "গত মাস"
        AnalyticsPeriod.THIS_YEAR -> "এই বছর"
        AnalyticsPeriod.CUSTOM -> "কাস্টম"
    }
}
