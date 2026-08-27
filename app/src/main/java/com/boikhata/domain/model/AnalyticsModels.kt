package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.boikhata.data.local.SyncStatus

/**
 * Analytics data model for business insights
 * Used for dashboard charts and reports
 */
data class AnalyticsData(
    val period: AnalyticsPeriod,
    val totalSales: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val profitMargin: Double,
    val salesTrend: List<SalesTrendPoint>,
    val topProducts: List<TopProduct>,
    val expenseBreakdown: Map<String, Double>,
    val customerCount: Int,
    val dueAmount: Double,
    val cashInHand: Double,
    val startDate: Long,
    val endDate: Long
)

/**
 * Sales trend point for chart visualization
 */
data class SalesTrendPoint(
    val timestamp: Long,
    val label: String,
    val sales: Double,
    val expenses: Double,
    val profit: Double
)

/**
 * Top selling product information
 */
data class TopProduct(
    val bookId: String,
    val titleBn: String,
    val titleEn: String?,
    val quantitySold: Int,
    val revenue: Double,
    val profit: Double,
    val stockRemaining: Int
)

/**
 * Analytics period filter options
 */
enum class AnalyticsPeriod {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    CUSTOM
}

/**
 * Expense category breakdown for analytics
 */
data class ExpenseCategoryBreakdown(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int
)

/**
 * Daily summary for dashboard
 */
data class DailySummary(
    val date: Long,
    val totalSales: Double,
    val totalCashReceived: Double,
    val totalDue: Double,
    val billCount: Int,
    val returnCount: Int,
    val newCustomers: Int,
    val topSellingBookId: String?,
    val openingBalance: Double,
    val closingBalance: Double
)

/**
 * Monthly performance metrics
 */
data class MonthlyPerformance(
    val month: Int,
    val year: Int,
    val totalSales: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val growthRate: Double?,
    val comparisonWithPreviousMonth: Double
)

/**
 * Inventory analytics data
 */
data class InventoryAnalytics(
    val totalBooks: Int,
    val lowStockItems: Int,
    val outOfStockItems: Int,
    val overstockedItems: Int,
    val totalInventoryValue: Double,
    val slowMovingItems: List<SlowMovingItem>,
    val fastMovingItems: List<FastMovingItem>
)

/**
 * Slow moving inventory item
 */
data class SlowMovingItem(
    val bookId: String,
    val title: String,
    val currentStock: Int,
    val lastSaleDate: Long?,
    val daysSinceLastSale: Int
)

/**
 * Fast moving inventory item
 */
data class FastMovingItem(
    val bookId: String,
    val title: String,
    val currentStock: Int,
    val avgMonthlySales: Int,
    val suggestedReorderQuantity: Int
)

/**
 * Customer analytics data
 */
data class CustomerAnalytics(
    val totalCustomers: Int,
    val activeCustomers: Int,
    val newCustomersThisMonth: Int,
    val topCustomers: List<TopCustomer>,
    val customersWithDue: Int,
    val totalDueAmount: Double
)

/**
 * Top customer by purchase volume
 */
data class TopCustomer(
    val customerId: String,
    val name: String,
    val totalPurchases: Double,
    val totalPaid: Double,
    val dueAmount: Double,
    val visitCount: Int
)

/**
 * Reorder suggestion for inventory management
 */
data class ReorderSuggestion(
    val bookId: String,
    val titleBn: String,
    val currentStock: Int,
    val suggestedQuantity: Int,
    val avgMonthlySales: Int,
    val priority: PriorityLevel,
    val estimatedCost: Double,
    val supplierId: String?
)

/**
 * Priority level for reorder suggestions
 */
enum class PriorityLevel {
    CRITICAL,  // Out of stock or very low
    HIGH,      // Below threshold
    MEDIUM,    // Approaching threshold
    LOW        // Normal stock but could optimize
}

/**
 * Cash flow statement data
 */
data class CashFlowStatement(
    val period: AnalyticsPeriod,
    val openingBalance: Double,
    val cashInflows: List<CashFlowItem>,
    val cashOutflows: List<CashFlowItem>,
    val totalInflow: Double,
    val totalOutflow: Double,
    val netCashFlow: Double,
    val closingBalance: Double
)

/**
 * Individual cash flow item
 */
data class CashFlowItem(
    val id: String,
    val date: Long,
    val description: String,
    val amount: Double,
    val type: CashFlowType,
    val category: String
)

enum class CashFlowType {
    INFLOW,
    OUTFLOW
}

/**
 * VAT report data for tax compliance
 */
data class VatReport(
    val period: AnalyticsPeriod,
    val totalSales: Double,
    val taxableSales: Double,
    val totalVatCollected: Double,
    val totalInputVat: Double,
    val netVatPayable: Double,
    val vatBreakdown: List<VatBreakdown>
)

data class VatBreakdown(
    val vatRate: Double,
    val salesAmount: Double,
    val vatAmount: Double
)
