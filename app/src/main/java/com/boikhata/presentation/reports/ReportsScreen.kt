package com.boikhata.presentation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.domain.model.report.ReportPeriod
import com.boikhata.domain.model.report.ReportData
import com.boikhata.domain.model.report.SalesReportData
import com.boikhata.domain.model.report.KhataReportData
import com.boikhata.domain.model.report.ExpenseReportData
import com.boikhata.domain.model.report.ProfitLossReportData
import com.boikhata.util.toBn

/**
 * Enterprise Reports Screen
 * Supports Sales, Khata, Expense, and Profit/Loss reports with PDF/CSV export
 */
@Composable
fun ReportsScreen(
    tenantId: String,
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedReportType by remember { mutableStateOf(ReportType.SALES) }
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var showPeriodSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("রিপোর্ট সেন্টার") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "পিছনে যান"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export PDF */ }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.FileDownload,
                            contentDescription = "PDF এক্সপোর্ট"
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
            // Report Type Selector
            ReportTypeSelector(
                selectedType = selectedReportType,
                onTypeSelected = { selectedReportType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Period Selector
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { 
                    selectedPeriod = it
                    generateReport(viewModel, tenantId, selectedReportType, it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Button
            Button(
                onClick = { generateReport(viewModel, tenantId, selectedReportType, selectedPeriod) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("রিপোর্ট তৈরি করুন")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Report Content
            when (val state = uiState) {
                is ReportsUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("রিপোর্ট দেখতে উপরের বাটন চাপুন")
                    }
                }
                is ReportsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.message)
                        }
                    }
                }
                is ReportsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ReportSummaryCard(state.report)
                        }
                    }
                }
                is ReportsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "ত্রুটি: ${state.message}",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTypeSelector(
    selectedType: ReportType,
    onTypeSelected: (ReportType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ReportType.entries.take(4).forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) }
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(ReportPeriod.entries.size) { index ->
            val period = ReportPeriod.entries[index]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(period.displayName)
                RadioButton(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) }
                )
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(report: ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "রিপোর্ট সারসংক্ষেপ",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            when (report) {
                is SalesReportData -> {
                    SummaryRow("মোট বিল", report.summary.totalBills.toBn())
                    SummaryRow("মোট বিক্রয়", "৳${report.summary.totalSales.toBn()}")
                    SummaryRow("মোট বকেয়া", "৳${report.summary.totalDue.toBn()}")
                    SummaryRow("গড় বিল", "৳${report.summary.averageBillValue.toBn()}")
                }
                is KhataReportData -> {
                    SummaryRow("মোট এন্ট্রি", report.summary.totalEntries.toBn())
                    SummaryRow("মোট ডেবিট", "৳${report.summary.totalDebit.toBn()}")
                    SummaryRow("মোট ক্রেডিট", "৳${report.summary.totalCredit.toBn()}")
                    SummaryRow("নিট ব্যালেন্স", "৳${report.summary.netBalance.toBn()}")
                }
                is ExpenseReportData -> {
                    SummaryRow("মোট খরচ", report.summary.totalExpenses.toBn())
                    SummaryRow("মোট পরিমাণ", "৳${report.summary.totalAmount.toBn()}")
                    SummaryRow("গড় দৈনিক", "৳${report.summary.averageDailyExpense.toBn()}")
                }
                is ProfitLossReportData -> {
                    SummaryRow("মোট আয়", "৳${report.summary.totalIncome.toBn()}")
                    SummaryRow("মোট খরচ", "৳${report.summary.totalExpense.toBn()}")
                    SummaryRow("নিট লাভ", "৳${report.summary.netProfit.toBn()}")
                    SummaryRow("লাভের হার", "${report.summary.profitMarginPercentage.toBn()}%")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun generateReport(
    viewModel: ReportsViewModel,
    tenantId: String,
    reportType: ReportType,
    period: ReportPeriod
) {
    when (reportType) {
        ReportType.SALES_REPORT -> viewModel.generateSalesReport(tenantId, period)
        ReportType.KHATA_REPORT -> viewModel.generateKhataReport(tenantId, period)
        ReportType.EXPENSE_REPORT -> viewModel.generateExpenseReport(tenantId, period)
        ReportType.PROFIT_LOSS_REPORT -> viewModel.generateProfitLossReport(tenantId, period)
        else -> { /* Other reports */ }
    }
}

enum class ReportType(val displayName: String) {
    SALES("বিক্রয়"),
    KHATA("খাতা"),
    EXPENSE("খরচ"),
    PROFIT_LOSS("লাভ-ক্ষতি"),
    STOCK("স্টক"),
    CASHBOOK("ক্যাশবুক")
}

private val ReportPeriod.displayName: String
    get() = when (this) {
        ReportPeriod.TODAY -> "আজ"
        ReportPeriod.YESTERDAY -> "গতকাল"
        ReportPeriod.THIS_WEEK -> "এই সপ্তাহ"
        ReportPeriod.LAST_WEEK -> "গত সপ্তাহ"
        ReportPeriod.THIS_MONTH -> "এই মাস"
        ReportPeriod.LAST_MONTH -> "গত মাস"
        ReportPeriod.THIS_QUARTER -> "এই ত্রৈমাসিক"
        ReportPeriod.LAST_QUARTER -> "গত ত্রৈমাসিক"
        ReportPeriod.THIS_YEAR -> "এই বছর"
        ReportPeriod.LAST_YEAR -> "গত বছর"
        ReportPeriod.CUSTOM -> "কাস্টম"
    }
