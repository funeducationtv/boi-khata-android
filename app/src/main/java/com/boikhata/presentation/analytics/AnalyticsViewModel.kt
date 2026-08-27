package com.boikhata.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Analytics Screen
 */
data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val analyticsData: AnalyticsData? = null,
    val inventoryAnalytics: InventoryAnalytics? = null,
    val customerAnalytics: CustomerAnalytics? = null,
    val reorderSuggestions: List<ReorderSuggestion> = emptyList(),
    val showReorderDialog: Boolean = false,
    val exportSuccess: Boolean = false
)

/**
 * ViewModel for Analytics Dashboard
 * Provides business intelligence data for decision making
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    /**
     * Load analytics data for selected period
     */
    fun loadAnalytics(period: AnalyticsPeriod = _uiState.value.selectedPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedPeriod = period)

            try {
                val analyticsData = analyticsRepository.getAnalyticsData(period)
                val inventoryAnalytics = analyticsRepository.getInventoryAnalytics()
                val customerAnalytics = analyticsRepository.getCustomerAnalytics()
                val reorderSuggestions = analyticsRepository.getReorderSuggestions()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analyticsData = analyticsData,
                    inventoryAnalytics = inventoryAnalytics,
                    customerAnalytics = customerAnalytics,
                    reorderSuggestions = reorderSuggestions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "ডেটা লোড করা যায়নি: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh all analytics data
     */
    fun refresh() {
        loadAnalytics(_uiState.value.selectedPeriod)
    }

    /**
     * Change the analytics period filter
     */
    fun setPeriod(period: AnalyticsPeriod) {
        if (period != _uiState.value.selectedPeriod) {
            loadAnalytics(period)
        }
    }

    /**
     * Export analytics report as CSV
     */
    fun exportReport() {
        viewModelScope.launch {
            try {
                val csvContent = analyticsRepository.exportAnalyticsCsv(_uiState.value.selectedPeriod)
                // TODO: Save to file and share
                _uiState.value = _uiState.value.copy(exportSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "রপ্তানি ব্যর্থ: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear export success flag
     */
    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }

    /**
     * Show reorder suggestions dialog
     */
    fun showReorderSuggestions() {
        _uiState.value = _uiState.value.copy(showReorderDialog = true)
    }

    /**
     * Hide reorder suggestions dialog
     */
    fun hideReorderSuggestions() {
        _uiState.value = _uiState.value.copy(showReorderDialog = false)
    }

    /**
     * Get profit margin display text
     */
    fun getProfitMarginText(): String {
        val margin = _uiState.value.analyticsData?.profitMargin ?: 0.0
        return "${margin.toBn()}%"
    }

    /**
     * Check if profit is positive
     */
    fun isProfitable(): Boolean {
        return (_uiState.value.analyticsData?.netProfit ?: 0.0) > 0
    }

    /**
     * Get low stock count for alert badge
     */
    fun getLowStockCount(): Int {
        return _uiState.value.inventoryAnalytics?.lowStockItems ?: 0
    }

    /**
     * Get total due amount from customers
     */
    fun getTotalDueAmount(): Double {
        return _uiState.value.analyticsData?.dueAmount ?: 0.0
    }
}
