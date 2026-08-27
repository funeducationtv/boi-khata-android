package com.boikhata.presentation.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.CashbookEntry
import com.boikhata.domain.repository.AccountingRepository
import com.boikhata.domain.repository.BillingRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Cash Close Screen
 */
data class CashCloseUiState(
    val totalSales: Double = 0.0,
    val totalCollection: Double = 0.0,
    val totalExpense: Double = 0.0,
    val systemCash: Double = 0.0,
    val recentEntries: List<CashbookEntry> = emptyList(),
    val isLoading: Boolean = false,
    val cashCloseSuccess: Boolean = false
)

/**
 * ViewModel for Cash Close functionality.
 * Calculates daily totals and handles cash reconciliation.
 */
@HiltViewModel
class CashCloseViewModel @Inject constructor(
    private val accountingRepo: AccountingRepository,
    private val billingRepo: BillingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashCloseUiState())
    val uiState: StateFlow<CashCloseUiState> = _uiState.asStateFlow()

    init {
        loadTodaySummary()
    }

    /**
     * Load today's summary data
     */
    private fun loadTodaySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val startOfDay = getStartOfDayTimestamp()
            val endOfDay = System.currentTimeMillis()
            val tenantId = sessionManager.currentTenant.value?.id ?: ""

            // Calculate total sales for today
            val todayBills = billingRepo.getAllBills(tenantId)
            val totalSales = todayBills.value
                .filter { it.billDate >= startOfDay && it.billDate <= endOfDay }
                .sumOf { it.totalAmount }

            // Calculate total collection (paid amount)
            val totalCollection = todayBills.value
                .filter { it.billDate >= startOfDay && it.billDate <= endOfDay }
                .sumOf { it.paidAmount }

            // Calculate total expenses for today
            val todayExpenses = accountingRepo.getExpenses()
            val totalExpense = todayExpenses.value
                .filter { it.expense.timestamp >= startOfDay && it.expense.timestamp <= endOfDay }
                .sumOf { it.expense.amount }

            // Calculate system cash (Opening + Collection - Expenses)
            val openingBalance = accountingRepo.getCashbookEntries(com.boikhata.domain.model.CashbookAccount.CASH)
                .value
                .filter { it.type == com.boikhata.domain.model.CashbookType.DEBIT }
                .sumOf { it.amount }

            val systemCash = openingBalance + totalCollection - totalExpense

            // Get recent entries
            val recentEntries = accountingRepo.getCashbookEntries(com.boikhata.domain.model.CashbookAccount.CASH)
                .value
                .take(10)

            _uiState.value = CashCloseUiState(
                totalSales = totalSales,
                totalCollection = totalCollection,
                totalExpense = totalExpense,
                systemCash = systemCash,
                recentEntries = recentEntries,
                isLoading = false
            )
        }
    }

    /**
     * Submit cash close with physical cash count
     */
    fun submitCashClose(physicalCash: Double) {
        viewModelScope.launch {
            val userId = sessionManager.currentUser.value?.id ?: return@launch
            val tenantId = sessionManager.currentTenant.value?.id ?: return@launch
            
            val difference = physicalCash - _uiState.value.systemCash
            
            // Create cash close entry in cashbook
            if (difference != 0.0) {
                val description = if (difference > 0) {
                    "ক্যাশ ক্লোজ - অতিরিক্ত: ৳${String.format("%.2f", difference)}"
                } else {
                    "ক্যাশ ক্লোজ - ঘাটতি: ৳${String.format("%.2f", Math.abs(difference))}"
                }
                
                accountingRepo.addCashbookEntry(
                    account = com.boikhata.domain.model.CashbookAccount.CASH,
                    type = if (difference > 0) com.boikhata.domain.model.CashbookType.CREDIT else com.boikhata.domain.model.CashbookType.DEBIT,
                    amount = Math.abs(difference),
                    description = description,
                    referenceId = null,
                    userId = userId
                )
            }
            
            // Mark cash close as completed
            _uiState.value = _uiState.value.copy(cashCloseSuccess = true)
            
            // Reload summary
            loadTodaySummary()
        }
    }

    /**
     * Get start of day timestamp (midnight)
     */
    private fun getStartOfDayTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadTodaySummary()
    }
}
