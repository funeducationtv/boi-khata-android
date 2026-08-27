package com.boikhata.data.repository.report

import com.boikhata.data.local.dao.Phase3Dao
import com.boikhata.data.local.dao.Phase4Dao
import com.boikhata.domain.model.Bill
import com.boikhata.domain.model.Expense
import com.boikhata.domain.model.KhataEntry
import com.boikhata.domain.model.StockLedgerEntry
import com.boikhata.domain.model.report.*
import com.boikhata.domain.repository.ReportRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val phase3Dao: Phase3Dao,
    private val phase4Dao: Phase4Dao
) : ReportRepository {

    override suspend fun generateSalesReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long?,
        endDate: Long?
    ): Result<SalesReportData> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = calculateDateRange(period, startDate, endDate)
            
            // Fetch bills from database
            val allBills = phase3Dao.getAllBills(tenantId).first()
            val filteredBills = allBills.filter { 
                it.billDate in start..end && !it.isDeleted 
            }
            
            // Calculate summary
            val totalSales = filteredBills.sumOf { it.totalAmount }
            val totalVat = filteredBills.sumOf { it.vatAmount }
            val totalDiscount = filteredBills.sumOf { it.discountAmount }
            val totalPaid = filteredBills.sumOf { it.paidAmount }
            val totalDue = filteredBills.sumOf { it.dueAmount }
            
            val summary = SalesReportSummary(
                totalBills = filteredBills.size,
                totalSales = totalSales,
                totalVat = totalVat,
                totalDiscount = totalDiscount,
                netSales = totalSales - totalDiscount,
                totalPaid = totalPaid,
                totalDue = totalDue,
                averageBillValue = if (filteredBills.isNotEmpty()) totalSales / filteredBills.size else 0.0,
                cashSales = totalPaid,
                dueSales = totalDue
            )
            
            // Get daily sales trend
            val dailySales = calculateDailySales(filteredBills, start, end)
            
            // Get top products
            val topProducts = getTopSellingProductsFromBills(filteredBills, 10)
            
            // Get customer-wise sales
            val customerWiseSales = calculateCustomerWiseSales(filteredBills)
            
            // Payment method breakdown (simplified - all cash for now)
            val paymentBreakdown = mapOf("Cash" to totalPaid, "Due" to totalDue)
            
            Result.success(
                SalesReportData(
                    period = period,
                    startDate = start,
                    endDate = end,
                    summary = summary,
                    bills = filteredBills.map { it.toBillSummary() },
                    dailySales = dailySales,
                    topProducts = topProducts,
                    customerWiseSales = customerWiseSales,
                    paymentMethodBreakdown = paymentBreakdown
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateKhataReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long?,
        endDate: Long?
    ): Result<KhataReportData> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = calculateDateRange(period, startDate, endDate)
            
            // Fetch Khata entries
            val allEntries = phase4Dao.getAllKhataEntries(tenantId).first()
            val filteredEntries = allEntries.filter { 
                it.date in start..end && !it.isDeleted 
            }
            
            val totalDebit = filteredEntries.filter { it.type == KhataEntryType.DEBIT }.sumOf { it.amount }
            val totalCredit = filteredEntries.filter { it.type == KhataEntryType.CREDIT }.sumOf { it.amount }
            
            val summary = KhataReportSummary(
                totalEntries = filteredEntries.size,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = totalDebit - totalCredit,
                totalCustomers = 0, // To be calculated
                customersWithDue = 0,
                overdueAmount = 0.0
            )
            
            Result.success(
                KhataReportData(
                    period = period,
                    startDate = start,
                    endDate = end,
                    summary = summary,
                    entries = emptyList(),
                    customerWiseBalance = emptyList(),
                    agingReport = AgingReportData(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateExpenseReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long?,
        endDate: Long?
    ): Result<ExpenseReportData> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = calculateDateRange(period, startDate, endDate)
            
            val allExpenses = phase4Dao.getAllExpenses(tenantId).first()
            val filteredExpenses = allExpenses.filter { 
                it.date in start..end && !it.isDeleted 
            }
            
            val totalAmount = filteredExpenses.sumOf { it.amount }
            val categoryWise = filteredExpenses.groupBy { it.category.name }
                .mapValues { it.value.sumOf { expense -> expense.amount } }
            
            val summary = ExpenseReportSummary(
                totalExpenses = filteredExpenses.size,
                totalAmount = totalAmount,
                averageDailyExpense = totalAmount / ((end - start) / 86400000 + 1),
                highestExpenseCategory = categoryWise.maxByOrNull { it.value }?.key ?: "",
                highestExpenseAmount = categoryWise.maxByOrNull { it.value }?.value ?: 0.0
            )
            
            Result.success(
                ExpenseReportData(
                    period = period,
                    startDate = start,
                    endDate = end,
                    summary = summary,
                    expenses = emptyList(),
                    categoryWiseExpense = categoryWise,
                    dailyExpenses = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateStockReport(
        tenantId: String,
        period: ReportPeriod
    ): Result<StockReportData> = withContext(Dispatchers.IO) {
        try {
            // Implementation for stock report
            Result.success(
                StockReportData(
                    period = period,
                    startDate = 0,
                    endDate = 0,
                    summary = StockReportSummary(0, 0.0, 0, 0, 0, 0),
                    stockItems = emptyList(),
                    lowStockItems = emptyList(),
                    outOfStockItems = emptyList(),
                    stockMovement = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateProfitLossReport(
        tenantId: String,
        period: ReportPeriod,
        startDate: Long?,
        endDate: Long?
    ): Result<ProfitLossReportData> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = calculateDateRange(period, startDate, endDate)
            
            // Calculate income from sales
            val salesReport = generateSalesReport(tenantId, period, startDate, endDate)
            val totalIncome = salesReport.getOrNull()?.summary?.netSales ?: 0.0
            
            // Calculate expenses
            val expenseReport = generateExpenseReport(tenantId, period, startDate, endDate)
            val totalExpense = expenseReport.getOrNull()?.summary?.totalAmount ?: 0.0
            
            val grossProfit = totalIncome - totalExpense
            val netProfit = grossProfit // Simplified - no taxes yet
            val profitMargin = if (totalIncome > 0) (netProfit / totalIncome * 100) else 0.0
            
            val summary = ProfitLossSummary(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                grossProfit = grossProfit,
                netProfit = netProfit,
                profitMarginPercentage = profitMargin
            )
            
            Result.success(
                ProfitLossReportData(
                    period = period,
                    startDate = start,
                    endDate = end,
                    summary = summary,
                    incomeBreakdown = mapOf("Sales" to totalIncome),
                    expenseBreakdown = emptyMap(),
                    monthlyTrend = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateCustomerStatement(
        tenantId: String,
        customerId: String,
        period: ReportPeriod
    ): Result<CustomerStatementData> {
        TODO("Not yet implemented")
    }

    override suspend fun getTopSellingProducts(
        tenantId: String,
        period: ReportPeriod,
        limit: Int
    ): Result<List<TopProductData>> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = calculateDateRange(period, null, null)
            val allBills = phase3Dao.getAllBills(tenantId).first()
            val filteredBills = allBills.filter { it.billDate in start..end && !it.isDeleted }
            
            Result.success(getTopSellingProductsFromBills(filteredBills, limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDailySalesTrend(
        tenantId: String,
        period: ReportPeriod
    ): Result<List<DailySalesData>> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = calculateDateRange(period, null, null)
            val allBills = phase3Dao.getAllBills(tenantId).first()
            val filteredBills = allBills.filter { it.billDate in start..end && !it.isDeleted }
            
            Result.success(calculateDailySales(filteredBills, start, end))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCustomerAgingReport(tenantId: String): Result<AgingReportData> {
        TODO("Not yet implemented")
    }

    override suspend fun getCategoryWiseExpenses(
        tenantId: String,
        period: ReportPeriod
    ): Result<Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val expenseReport = generateExpenseReport(tenantId, period, null, null)
            expenseReport.map { it.categoryWiseExpense }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLowStockItems(tenantId: String): Result<List<StockItemData>> {
        TODO("Not yet implemented")
    }

    // Helper functions
    private fun calculateDateRange(
        period: ReportPeriod,
        startDate: Long?,
        endDate: Long?
    ): Pair<Long, Long> {
        if (startDate != null && endDate != null) {
            return startDate to endDate
        }
        
        val calendar = Calendar.getInstance()
        val end = System.currentTimeMillis()
        
        val start = when (period) {
            ReportPeriod.TODAY -> end - 86400000
            ReportPeriod.YESTERDAY -> end - 2 * 86400000
            ReportPeriod.THIS_WEEK -> end - 7 * 86400000
            ReportPeriod.LAST_WEEK -> end - 14 * 86400000
            ReportPeriod.THIS_MONTH -> end - 30 * 86400000
            ReportPeriod.LAST_MONTH -> end - 60 * 86400000
            ReportPeriod.THIS_QUARTER -> end - 90 * 86400000
            ReportPeriod.LAST_QUARTER -> end - 180 * 86400000
            ReportPeriod.THIS_YEAR -> end - 365 * 86400000
            ReportPeriod.LAST_YEAR -> end - 730 * 86400000
            ReportPeriod.CUSTOM -> end - 30 * 86400000
        }
        
        return start to end
    }
    
    private fun Bill.toBillSummary(): BillSummary {
        return BillSummary(
            billId = this.id,
            billNumber = this.billNumber,
            date = this.billDate,
            customerName = this.customerNameBn,
            subtotal = this.subtotal,
            vat = this.vatAmount,
            discount = this.discountAmount,
            total = this.totalAmount,
            paid = this.paidAmount,
            due = this.dueAmount,
            status = this.syncStatus.name
        )
    }
    
    private fun calculateDailySales(bills: List<Bill>, start: Long, end: Long): List<DailySalesData> {
        val dailyMap = bills.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.billDate
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        
        return dailyMap.map { (date, dayBills) ->
            DailySalesData(
                date = date,
                salesAmount = dayBills.sumOf { it.totalAmount },
                billCount = dayBills.size,
                averageBillValue = dayBills.sumOf { it.totalAmount } / dayBills.size
            )
        }.sortedBy { it.date }
    }
    
    private fun getTopSellingProductsFromBills(bills: List<Bill>, limit: Int): List<TopProductData> {
        // Simplified implementation - needs bill lines data
        return emptyList()
    }
    
    private fun calculateCustomerWiseSales(bills: List<Bill>): List<CustomerWiseSalesData> {
        val customerMap = bills.groupBy { it.customerId }
        return customerMap.map { (customerId, customerBills) ->
            CustomerWiseSalesData(
                customerId = customerId ?: "",
                customerName = customerBills.firstOrNull()?.customerNameBn ?: "",
                totalPurchases = customerBills.sumOf { it.totalAmount },
                billCount = customerBills.size,
                currentDue = customerBills.sumOf { it.dueAmount }
            )
        }
    }
}
