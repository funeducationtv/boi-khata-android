package com.boikhata.domain.repository

import com.boikhata.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for analytics operations
 * Provides business intelligence and reporting data
 */
interface AnalyticsRepository {

    /**
     * Get comprehensive analytics data for a specific period
     */
    suspend fun getAnalyticsData(period: AnalyticsPeriod): AnalyticsData

    /**
     * Get analytics data for custom date range
     */
    suspend fun getAnalyticsDataForRange(startDate: Long, endDate: Long): AnalyticsData

    /**
     * Get sales trend data points for chart visualization
     */
    suspend fun getSalesTrend(period: AnalyticsPeriod): List<SalesTrendPoint>

    /**
     * Get top selling products for a period
     */
    suspend fun getTopProducts(period: AnalyticsPeriod, limit: Int = 10): List<TopProduct>

    /**
     * Get expense breakdown by category
     */
    suspend fun getExpenseBreakdown(period: AnalyticsPeriod): Map<String, Double>

    /**
     * Get daily summary for dashboard
     */
    suspend fun getDailySummary(date: Long): DailySummary

    /**
     * Get monthly performance metrics
     */
    suspend fun getMonthlyPerformance(year: Int): List<MonthlyPerformance>

    /**
     * Get inventory analytics
     */
    suspend fun getInventoryAnalytics(): InventoryAnalytics

    /**
     * Get customer analytics
     */
    suspend fun getCustomerAnalytics(): CustomerAnalytics

    /**
     * Get reorder suggestions based on stock levels and sales velocity
     */
    suspend fun getReorderSuggestions(): List<ReorderSuggestion>

    /**
     * Get cash flow statement for a period
     */
    suspend fun getCashFlowStatement(period: AnalyticsPeriod): CashFlowStatement

    /**
     * Get VAT report for tax compliance
     */
    suspend fun getVatReport(period: AnalyticsPeriod): VatReport

    /**
     * Get slow moving inventory items
     */
    suspend fun getSlowMovingItems(daysThreshold: Int = 30): List<SlowMovingItem>

    /**
     * Get fast moving inventory items
     */
    suspend fun getFastMovingItems(): List<FastMovingItem>

    /**
     * Get top customers by purchase volume
     */
    suspend fun getTopCustomers(limit: Int = 10): List<TopCustomer>

    /**
     * Get customers with outstanding dues
     */
    suspend fun getCustomersWithDue(): List<TopCustomer>

    /**
     * Flow of daily summaries for the current month
     */
    fun getDailySummariesFlow(): Flow<List<DailySummary>>

    /**
     * Calculate profit margin for a period
     */
    suspend fun calculateProfitMargin(period: AnalyticsPeriod): Double

    /**
     * Calculate growth rate compared to previous period
     */
    suspend fun calculateGrowthRate(currentPeriod: AnalyticsPeriod): Double?

    /**
     * Get total due amount from all customers
     */
    suspend fun getTotalDueAmount(): Double

    /**
     * Get cash in hand from cashbook
     */
    suspend fun getCashInHand(): Double

    /**
     * Export analytics data as CSV
     */
    suspend fun exportAnalyticsCsv(period: AnalyticsPeriod): String

    /**
     * Clear analytics cache
     */
    fun clearCache()
}
