package com.boikhata.domain.repository

import com.boikhata.domain.model.report.*

/**
 * Repository interface for report generation and management
 */
interface ReportRepository {
    
    /**
     * Generate sales report for a specific period
     */
    suspend fun generateSalesReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<SalesReportData>
    
    /**
     * Generate Khata (credit/debit) report
     */
    suspend fun generateKhataReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<KhataReportData>
    
    /**
     * Generate expense report
     */
    suspend fun generateExpenseReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<ExpenseReportData>
    
    /**
     * Generate stock report
     */
    suspend fun generateStockReport(
        tenantId: String,
        period: ReportPeriod
    ): Result<StockReportData>
    
    /**
     * Generate Profit & Loss report
     */
    suspend fun generateProfitLossReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<ProfitLossReportData>
    
    /**
     * Generate customer statement
     */
    suspend fun generateCustomerStatement(
        tenantId: String,
        customerId: String,
        period: ReportPeriod
    ): Result<CustomerStatementData>
    
    /**
     * Get top selling products for a period
     */
    suspend fun getTopSellingProducts(
        tenantId: String,
        period: ReportPeriod,
        limit: Int = 10
    ): Result<List<TopProductData>>
    
    /**
     * Get daily sales trend
     */
    suspend fun getDailySalesTrend(
        tenantId: String,
        period: ReportPeriod
    ): Result<List<DailySalesData>>
    
    /**
     * Get customer aging report (overdue analysis)
     */
    suspend fun getCustomerAgingReport(
        tenantId: String
    ): Result<AgingReportData>
    
    /**
     * Get category-wise expense breakdown
     */
    suspend fun getCategoryWiseExpenses(
        tenantId: String,
        period: ReportPeriod
    ): Result<Map<String, Double>>
    
    /**
     * Get low stock items alert
     */
    suspend fun getLowStockItems(
        tenantId: String
    ): Result<List<StockItemData>>
}
