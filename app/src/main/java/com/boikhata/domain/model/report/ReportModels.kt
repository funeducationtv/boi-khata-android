package com.boikhata.domain.model.report

import com.boikhata.domain.model.Bill
import com.boikhata.domain.model.KhataEntry
import com.boikhata.domain.model.Expense
import com.boikhata.domain.model.StockLedgerEntry

/**
 * Report types supported by the system
 */
enum class ReportType {
    SALES_REPORT,
    PURCHASE_REPORT,
    KHATA_REPORT,
    EXPENSE_REPORT,
    STOCK_REPORT,
    CASHBOOK_REPORT,
    PROFIT_LOSS_REPORT,
    TAX_REPORT,
    CUSTOMER_STATEMENT,
    SUPPLIER_STATEMENT
}

/**
 * Report period filter
 */
enum class ReportPeriod {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_QUARTER,
    LAST_QUARTER,
    THIS_YEAR,
    LAST_YEAR,
    CUSTOM
}

/**
 * Export format for reports
 */
enum class ExportFormat {
    PDF,
    EXCEL,
    CSV,
    PRINT
}

/**
 * Base report data structure
 */
sealed class ReportData {
    abstract val reportType: ReportType
    abstract val period: ReportPeriod
    abstract val startDate: Long
    abstract val endDate: Long
    abstract val generatedAt: Long
    abstract val summary: ReportSummary
}

/**
 * Sales report data
 */
data class SalesReportData(
    override val reportType: ReportType = ReportType.SALES_REPORT,
    override val period: ReportPeriod,
    override val startDate: Long,
    override val endDate: Long,
    override val generatedAt: Long = System.currentTimeMillis(),
    override val summary: SalesReportSummary,
    val bills: List<BillSummary>,
    val dailySales: List<DailySalesData>,
    val topProducts: List<TopProductData>,
    val customerWiseSales: List<CustomerWiseSalesData>,
    val paymentMethodBreakdown: Map<String, Double>
) : ReportData()

data class SalesReportSummary(
    val totalBills: Int,
    val totalSales: Double,
    val totalVat: Double,
    val totalDiscount: Double,
    val netSales: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val averageBillValue: Double,
    val cashSales: Double,
    val dueSales: Double
) : ReportSummary

/**
 * Khata report data
 */
data class KhataReportData(
    override val reportType: ReportType = ReportType.KHATA_REPORT,
    override val period: ReportPeriod,
    override val startDate: Long,
    override val endDate: Long,
    override val generatedAt: Long = System.currentTimeMillis(),
    override val summary: KhataReportSummary,
    val entries: List<KhataEntrySummary>,
    val customerWiseBalance: List<CustomerBalanceData>,
    val agingReport: AgingReportData
) : ReportData()

data class KhataReportSummary(
    val totalEntries: Int,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double,
    val totalCustomers: Int,
    val customersWithDue: Int,
    val overdueAmount: Double
) : ReportSummary

/**
 * Expense report data
 */
data class ExpenseReportData(
    override val reportType: ReportType = ReportType.EXPENSE_REPORT,
    override val period: ReportPeriod,
    override val startDate: Long,
    override val endDate: Long,
    override val generatedAt: Long = System.currentTimeMillis(),
    override val summary: ExpenseReportSummary,
    val expenses: List<ExpenseSummary>,
    val categoryWiseExpense: Map<String, Double>,
    val dailyExpenses: List<DailyExpenseData>
) : ReportData()

data class ExpenseReportSummary(
    val totalExpenses: Int,
    val totalAmount: Double,
    val averageDailyExpense: Double,
    val highestExpenseCategory: String,
    val highestExpenseAmount: Double
) : ReportSummary

/**
 * Stock report data
 */
data class StockReportData(
    override val reportType: ReportType = ReportType.STOCK_REPORT,
    override val period: ReportPeriod,
    override val startDate: Long,
    override val endDate: Long,
    override val generatedAt: Long = System.currentTimeMillis(),
    override val summary: StockReportSummary,
    val stockItems: List<StockItemData>,
    val lowStockItems: List<StockItemData>,
    val outOfStockItems: List<StockItemData>,
    val stockMovement: List<StockMovementData>
) : ReportData()

data class StockReportSummary(
    val totalItems: Int,
    val totalStockValue: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val totalStockIn: Int,
    val totalStockOut: Int
) : ReportSummary

/**
 * Profit & Loss report data
 */
data class ProfitLossReportData(
    override val reportType: ReportType = ReportType.PROFIT_LOSS_REPORT,
    override val period: ReportPeriod,
    override val startDate: Long,
    override val endDate: Long,
    override val generatedAt: Long = System.currentTimeMillis(),
    override val summary: ProfitLossSummary,
    val incomeBreakdown: Map<String, Double>,
    val expenseBreakdown: Map<String, Double>,
    val monthlyTrend: List<MonthlyProfitLossData>
) : ReportData()

data class ProfitLossSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val grossProfit: Double,
    val netProfit: Double,
    val profitMarginPercentage: Double
) : ReportSummary

/**
 * Customer statement data
 */
data class CustomerStatementData(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val openingBalance: Double,
    val closingBalance: Double,
    val totalDebit: Double,
    val totalCredit: Double,
    val transactions: List<CustomerTransactionData>,
    val statementPeriod: ReportPeriod,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Supporting data classes
 */
data class BillSummary(
    val billId: String,
    val billNumber: String,
    val date: Long,
    val customerName: String,
    val subtotal: Double,
    val vat: Double,
    val discount: Double,
    val total: Double,
    val paid: Double,
    val due: Double,
    val status: String
)

data class DailySalesData(
    val date: Long,
    val salesAmount: Double,
    val billCount: Int,
    val averageBillValue: Double
)

data class TopProductData(
    val bookId: String,
    val title: String,
    val quantitySold: Int,
    val revenue: Double,
    val profit: Double
)

data class CustomerWiseSalesData(
    val customerId: String,
    val customerName: String,
    val totalPurchases: Double,
    val billCount: Int,
    val currentDue: Double
)

data class KhataEntrySummary(
    val entryId: String,
    val date: Long,
    val customerName: String,
    val type: String,
    val amount: Double,
    val balance: Double,
    val description: String
)

data class CustomerBalanceData(
    val customerId: String,
    val customerName: String,
    val phone: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val currentBalance: Double,
    val lastTransactionDate: Long,
    val isOverdue: Boolean,
    val daysOverdue: Int
)

data class AgingReportData(
    val current: Double,
    val days1to30: Double,
    val days31to60: Double,
    val days61to90: Double,
    val daysAbove90: Double,
    val totalReceivable: Double
)

data class ExpenseSummary(
    val expenseId: String,
    val date: Long,
    val category: String,
    val amount: Double,
    val description: String,
    val userName: String
)

data class DailyExpenseData(
    val date: Long,
    val expenseAmount: Double,
    val expenseCount: Int
)

data class StockItemData(
    val bookId: String,
    val title: String,
    val isbn: String?,
    val currentStock: Int,
    val unitPrice: Double,
    val purchasePrice: Double,
    val stockValue: Double,
    val lowStockThreshold: Int,
    val status: StockStatus
)

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}

data class StockMovementData(
    val date: Long,
    val bookTitle: String,
    val movementType: String,
    val quantity: Int,
    val balanceAfter: Int,
    val reference: String
)

data class MonthlyProfitLossData(
    val month: String,
    val income: Double,
    val expense: Double,
    val profit: Double,
    val profitMargin: Double
)

data class CustomerTransactionData(
    val date: Long,
    val type: String,
    val description: String,
    val debit: Double,
    val credit: Double,
    val balance: Double,
    val reference: String
)

/**
 * Report summary interface
 */
interface ReportSummary
