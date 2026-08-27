package com.boikhata.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.report.*
import com.boikhata.domain.repository.ReportRepository
import com.boikhata.util.export.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for managing report generation and export
 * Supports Sales, Khata, Expense, Stock, and Profit/Loss reports
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val reportExporter: ReportExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Idle)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private var currentSalesReport: SalesReportData? = null
    private var currentKhataReport: KhataReportData? = null
    private var currentExpenseReport: ExpenseReportData? = null
    private var currentProfitLossReport: ProfitLossReportData? = null

    /**
     * Generate sales report for selected period
     */
    fun generateSalesReport(tenantId: String, period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading("বিক্রয় রিপোর্ট তৈরি হচ্ছে...")
            
            reportRepository.generateSalesReport(tenantId, period).fold(
                onSuccess = { report ->
                    currentSalesReport = report
                    _uiState.value = ReportsUiState.Success(report)
                },
                onFailure = { error ->
                    _uiState.value = ReportsUiState.Error(error.message ?: "রিপোর্ট তৈরি ব্যর্থ")
                }
            )
        }
    }

    /**
     * Generate Khata (credit/debit) report
     */
    fun generateKhataReport(tenantId: String, period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading("খাতা রিপোর্ট তৈরি হচ্ছে...")
            
            reportRepository.generateKhataReport(tenantId, period).fold(
                onSuccess = { report ->
                    currentKhataReport = report
                    _uiState.value = ReportsUiState.Success(report)
                },
                onFailure = { error ->
                    _uiState.value = ReportsUiState.Error(error.message ?: "রিপোর্ট তৈরি ব্যর্থ")
                }
            )
        }
    }

    /**
     * Generate expense report
     */
    fun generateExpenseReport(tenantId: String, period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading("খরচ রিপোর্ট তৈরি হচ্ছে...")
            
            reportRepository.generateExpenseReport(tenantId, period).fold(
                onSuccess = { report ->
                    currentExpenseReport = report
                    _uiState.value = ReportsUiState.Success(report)
                },
                onFailure = { error ->
                    _uiState.value = ReportsUiState.Error(error.message ?: "রিপোর্ট তৈরি ব্যর্থ")
                }
            )
        }
    }

    /**
     * Generate Profit & Loss report
     */
    fun generateProfitLossReport(tenantId: String, period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading("লাভ-ক্ষতি রিপোর্ট তৈরি হচ্ছে...")
            
            reportRepository.generateProfitLossReport(tenantId, period).fold(
                onSuccess = { report ->
                    currentProfitLossReport = report
                    _uiState.value = ReportsUiState.Success(report)
                },
                onFailure = { error ->
                    _uiState.value = ReportsUiState.Error(error.message ?: "রিপোর্ট তৈরি ব্যর্থ")
                }
            )
        }
    }

    /**
     * Export current report to PDF
     */
    fun exportToPdf(): Result<File> {
        return when {
            currentSalesReport != null -> reportExporter.exportSalesReportPdf(currentSalesReport!!)
            currentKhataReport != null -> reportExporter.exportKhataReportPdf(currentKhataReport!!)
            currentExpenseReport != null -> reportExporter.exportExpenseReportPdf(currentExpenseReport!!)
            currentProfitLossReport != null -> reportExporter.exportProfitLossReportPdf(currentProfitLossReport!!)
            else -> Result.failure(Exception("কোনো রিপোর্ট নেই"))
        }
    }

    /**
     * Export current report to CSV
     */
    fun exportToCsv(): Result<File> {
        val report = when {
            currentSalesReport != null -> currentSalesReport
            currentKhataReport != null -> currentKhataReport
            currentExpenseReport != null -> currentExpenseReport
            currentProfitLossReport != null -> currentProfitLossReport
            else -> null
        }
        
        return report?.let { reportExporter.exportToCsv(it) }
            ?: Result.failure(Exception("কোনো রিপোর্ট নেই"))
    }

    /**
     * Share exported report
     */
    fun shareReport(file: File, reportType: String): Result<Unit> {
        return reportExporter.shareReport(file, reportType)
    }

    /**
     * Reset current report state
     */
    fun reset() {
        currentSalesReport = null
        currentKhataReport = null
        currentExpenseReport = null
        currentProfitLossReport = null
        _uiState.value = ReportsUiState.Idle
    }
}

/**
 * UI State for Reports screen
 */
sealed class ReportsUiState {
    object Idle : ReportsUiState()
    data class Loading(val message: String) : ReportsUiState()
    data class Success(val report: ReportData) : ReportsUiState()
    data class Error(val message: String) : ReportsUiState()
}
