package com.boikhata.data.repository

import com.boikhata.data.local.dao.BillingDao
import com.boikhata.data.local.dao.AccountingDao
import com.boikhata.data.local.dao.CatalogDao
import com.boikhata.data.local.dao.KhataDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnalyticsRepository
 * Provides business intelligence by aggregating data from various sources
 */
@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val billingDao: BillingDao,
    private val accountingDao: AccountingDao,
    private val catalogDao: CatalogDao,
    private val khataDao: KhataDao
) : AnalyticsRepository {

    override suspend fun getAnalyticsData(period: AnalyticsPeriod): AnalyticsData {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = getDateRange(period)

        val totalSales = billingDao.getTotalSalesBetween(startDate, endDate)
        val totalExpenses = accountingDao.getTotalExpensesBetween(startDate, endDate)
        val netProfit = totalSales - totalExpenses
        val profitMargin = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0

        val salesTrend = getSalesTrend(period)
        val topProducts = getTopProducts(period)
        val expenseBreakdown = getExpenseBreakdown(period)

        val customerCount = khataDao.getTotalCustomerCount()
        val dueAmount = getTotalDueAmount()
        val cashInHand = getCashInHand()

        return AnalyticsData(
            period = period,
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMargin = profitMargin,
            salesTrend = salesTrend,
            topProducts = topProducts,
            expenseBreakdown = expenseBreakdown,
            customerCount = customerCount,
            dueAmount = dueAmount,
            cashInHand = cashInHand,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getAnalyticsDataForRange(startDate: Long, endDate: Long): AnalyticsData {
        val totalSales = billingDao.getTotalSalesBetween(startDate, endDate)
        val totalExpenses = accountingDao.getTotalExpensesBetween(startDate, endDate)
        val netProfit = totalSales - totalExpenses
        val profitMargin = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0

        val salesTrend = calculateSalesTrendForRange(startDate, endDate)
        val topProducts = calculateTopProductsForRange(startDate, endDate)
        val expenseBreakdown = calculateExpenseBreakdownForRange(startDate, endDate)

        val customerCount = khataDao.getTotalCustomerCount()
        val dueAmount = getTotalDueAmount()
        val cashInHand = getCashInHand()

        return AnalyticsData(
            period = AnalyticsPeriod.CUSTOM,
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMargin = profitMargin,
            salesTrend = salesTrend,
            topProducts = topProducts,
            expenseBreakdown = expenseBreakdown,
            customerCount = customerCount,
            dueAmount = dueAmount,
            cashInHand = cashInHand,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getSalesTrend(period: AnalyticsPeriod): List<SalesTrendPoint> {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = getDateRange(period)

        return when (period) {
            AnalyticsPeriod.TODAY -> {
                // Hourly trend for today
                (0..23).map { hour ->
                    val hourStart = startDate + (hour * 3600000)
                    val hourEnd = hourStart + 3600000
                    val sales = billingDao.getSalesBetween(hourStart, hourEnd)
                    val expenses = accountingDao.getExpensesBetween(hourStart, hourEnd)
                    SalesTrendPoint(
                        timestamp = hourStart,
                        label = "${hour}:00",
                        sales = sales,
                        expenses = expenses,
                        profit = sales - expenses
                    )
                }
            }
            AnalyticsPeriod.THIS_WEEK, AnalyticsPeriod.LAST_WEEK -> {
                // Daily trend for the week
                (0..6).map { day ->
                    val dayStart = startDate + (day * 86400000)
                    val dayEnd = dayStart + 86400000
                    val sales = billingDao.getSalesBetween(dayStart, dayEnd)
                    val expenses = accountingDao.getExpensesBetween(dayStart, dayEnd)
                    SalesTrendPoint(
                        timestamp = dayStart,
                        label = getDayName(day),
                        sales = sales,
                        expenses = expenses,
                        profit = sales - expenses
                    )
                }
            }
            else -> {
                // Monthly trend
                (0..11).map { month ->
                    val monthStart = startDate + (month * 2592000000L) // Approx 30 days
                    val monthEnd = monthStart + 2592000000L
                    val sales = billingDao.getSalesBetween(monthStart, monthEnd)
                    val expenses = accountingDao.getExpensesBetween(monthStart, monthEnd)
                    SalesTrendPoint(
                        timestamp = monthStart,
                        label = getMonthName(month),
                        sales = sales,
                        expenses = expenses,
                        profit = sales - expenses
                    )
                }.filter { it.sales > 0 || it.expenses > 0 }
            }
        }
    }

    override suspend fun getTopProducts(period: AnalyticsPeriod, limit: Int): List<TopProduct> {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = getDateRange(period)

        val billLines = billingDao.getBillLinesBetween(startDate, endDate)
        
        // Group by book and calculate totals
        val bookSales = mutableMapOf<String, Pair<Int, Double>>()
        billLines.forEach { line ->
            val current = bookSales[line.bookId] ?: Pair(0, 0.0)
            bookSales[line.bookId] = Pair(
                current.first + line.quantity,
                current.second + line.lineTotal
            )
        }

        // Get book details and create TopProduct list
        return bookSales.entries
            .sortedByDescending { it.value.second }
            .take(limit)
            .mapNotNull { (bookId, sales) ->
                val book = catalogDao.getBookById(bookId) ?: return@mapNotNull null
                val stock = catalogDao.getStockForBook(bookId)
                TopProduct(
                    bookId = bookId,
                    titleBn = book.titleBn,
                    titleEn = book.titleEn,
                    quantitySold = sales.first,
                    revenue = sales.second,
                    profit = sales.second * 0.2, // Simplified: assume 20% margin
                    stockRemaining = stock
                )
            }
    }

    override suspend fun getExpenseBreakdown(period: AnalyticsPeriod): Map<String, Double> {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = getDateRange(period)

        val expenses = accountingDao.getExpensesBetween(startDate, endDate)
        val categoryTotals = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            val current = categoryTotals[expense.category] ?: 0.0
            categoryTotals[expense.category] = current + expense.amount
        }

        return categoryTotals
    }

    override suspend fun getDailySummary(date: Long): DailySummary {
        val dayStart = date - (date % 86400000)
        val dayEnd = dayStart + 86400000

        val totalSales = billingDao.getTotalSalesBetween(dayStart, dayEnd)
        val totalCashReceived = billingDao.getTotalPaidBetween(dayStart, dayEnd)
        val totalDue = totalSales - totalCashReceived
        val billCount = billingDao.getBillCountBetween(dayStart, dayEnd)
        val returnCount = 0 // TODO: Implement returns
        val newCustomers = khataDao.getNewCustomersBetween(dayStart, dayEnd)

        // Get opening and closing balance from cashbook
        val openingBalance = accountingDao.getCashbookBalanceBefore(dayStart)
        val closingBalance = accountingDao.getCashbookBalanceAt(dayEnd)

        return DailySummary(
            date = date,
            totalSales = totalSales,
            totalCashReceived = totalCashReceived,
            totalDue = totalDue,
            billCount = billCount,
            returnCount = returnCount,
            newCustomers = newCustomers,
            topSellingBookId = null, // TODO: Calculate
            openingBalance = openingBalance,
            closingBalance = closingBalance
        )
    }

    override suspend fun getMonthlyPerformance(year: Int): List<MonthlyPerformance> {
        val startDate = getStartOfYear(year)
        val endDate = getEndOfYear(year)

        return (0..11).mapNotNull { month ->
            val monthStart = startDate + (month * 2592000000L)
            val monthEnd = if (month < 11) monthStart + 2592000000L else endDate

            val totalSales = billingDao.getTotalSalesBetween(monthStart, monthEnd)
            val totalExpenses = accountingDao.getTotalExpensesBetween(monthStart, monthEnd)
            val netProfit = totalSales - totalExpenses

            // Calculate growth rate compared to previous month
            val growthRate = if (month > 0) {
                val prevMonthStart = startDate + ((month - 1) * 2592000000L)
                val prevMonthEnd = monthStart
                val prevSales = billingDao.getTotalSalesBetween(prevMonthStart, prevMonthEnd)
                if (prevSales > 0) ((totalSales - prevSales) / prevSales) * 100 else null
            } else null

            MonthlyPerformance(
                month = month + 1,
                year = year,
                totalSales = totalSales,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                growthRate = growthRate,
                comparisonWithPreviousMonth = growthRate ?: 0.0
            )
        }
    }

    override suspend fun getInventoryAnalytics(): InventoryAnalytics {
        val allBooks = catalogDao.getAllBooks()
        val lowStockItems = allBooks.count { it.currentStock <= it.book.lowStockThreshold }
        val outOfStockItems = allBooks.count { it.currentStock == 0 }
        val overstockedItems = allBooks.count { it.currentStock > it.book.lowStockThreshold * 5 }

        val totalInventoryValue = allBooks.sumOf { 
            it.currentStock * it.book.purchasePrice 
        }

        val slowMoving = getSlowMovingItems()
        val fastMoving = getFastMovingItems()

        return InventoryAnalytics(
            totalBooks = allBooks.size,
            lowStockItems = lowStockItems,
            outOfStockItems = outOfStockItems,
            overstockedItems = overstockedItems,
            totalInventoryValue = totalInventoryValue,
            slowMovingItems = slowMoving,
            fastMovingItems = fastMoving
        )
    }

    override suspend fun getCustomerAnalytics(): CustomerAnalytics {
        val totalCustomers = khataDao.getTotalCustomerCount()
        val activeCustomers = khataDao.getActiveCustomerCount()
        val newCustomersThisMonth = khataDao.getNewCustomersThisMonth()
        val customersWithDueList = getCustomersWithDue()
        val totalDueAmount = getTotalDueAmount()

        return CustomerAnalytics(
            totalCustomers = totalCustomers,
            activeCustomers = activeCustomers,
            newCustomersThisMonth = newCustomersThisMonth,
            topCustomers = getTopCustomers(),
            customersWithDue = customersWithDueList.size,
            totalDueAmount = totalDueAmount
        )
    }

    override suspend fun getReorderSuggestions(): List<ReorderSuggestion> {
        val allBooks = catalogDao.getAllBooks()
        
        return allBooks
            .filter { it.currentStock <= it.book.lowStockThreshold * 2 }
            .map { stockItem ->
                val avgMonthlySales = calculateAverageMonthlySales(stockItem.book.id)
                val suggestedQuantity = (avgMonthlySales * 2) - stockItem.currentStock
                val priority = when {
                    stockItem.currentStock == 0 -> PriorityLevel.CRITICAL
                    stockItem.currentStock <= stockItem.book.lowStockThreshold -> PriorityLevel.HIGH
                    stockItem.currentStock <= stockItem.book.lowStockThreshold * 1.5 -> PriorityLevel.MEDIUM
                    else -> PriorityLevel.LOW
                }

                ReorderSuggestion(
                    bookId = stockItem.book.id,
                    titleBn = stockItem.book.titleBn,
                    currentStock = stockItem.currentStock,
                    suggestedQuantity = maxOf(1, suggestedQuantity),
                    avgMonthlySales = avgMonthlySales,
                    priority = priority,
                    estimatedCost = suggestedQuantity * stockItem.book.purchasePrice,
                    supplierId = null // TODO: Add supplier mapping
                )
            }
            .sortedBy { it.priority.ordinal }
    }

    override suspend fun getCashFlowStatement(period: AnalyticsPeriod): CashFlowStatement {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = getDateRange(period)

        val inflows = billingDao.getBillsBetween(startDate, endDate)
            .map { bill ->
                CashFlowItem(
                    id = bill.id,
                    date = bill.billDate,
                    description = "বিল #${bill.billNumber}",
                    amount = bill.paidAmount,
                    type = CashFlowType.INFLOW,
                    category = "Sales"
                )
            }

        val outflows = accountingDao.getExpensesBetween(startDate, endDate)
            .map { expense ->
                CashFlowItem(
                    id = expense.id,
                    date = expense.date,
                    description = expense.description,
                    amount = expense.amount,
                    type = CashFlowType.OUTFLOW,
                    category = expense.category
                )
            }

        val totalInflow = inflows.sumOf { it.amount }
        val totalOutflow = outflows.sumOf { it.amount }
        val netCashFlow = totalInflow - totalOutflow

        val openingBalance = getCashInHand() // Simplified
        val closingBalance = openingBalance + netCashFlow

        return CashFlowStatement(
            period = period,
            openingBalance = openingBalance,
            cashInflows = inflows,
            cashOutflows = outflows,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netCashFlow = netCashFlow,
            closingBalance = closingBalance
        )
    }

    override suspend fun getVatReport(period: AnalyticsPeriod): VatReport {
        val now = System.currentTimeMillis()
        val (startDate, endDate) = getDateRange(period)

        val bills = billingDao.getBillsBetween(startDate, endDate)
        val totalSales = bills.sumOf { it.totalAmount }
        val taxableSales = bills.sumOf { it.subtotal }
        val totalVatCollected = bills.sumOf { it.vatAmount }

        // TODO: Calculate input VAT from purchases
        val totalInputVat = 0.0
        val netVatPayable = totalVatCollected - totalInputVat

        // Group by VAT rate
        val vatBreakdown = bills
            .groupBy { it.vatRate }
            .map { (rate, billsForRate) ->
                VatBreakdown(
                    vatRate = rate,
                    salesAmount = billsForRate.sumOf { it.subtotal },
                    vatAmount = billsForRate.sumOf { it.vatAmount }
                )
            }

        return VatReport(
            period = period,
            totalSales = totalSales,
            taxableSales = taxableSales,
            totalVatCollected = totalVatCollected,
            totalInputVat = totalInputVat,
            netVatPayable = netVatPayable,
            vatBreakdown = vatBreakdown
        )
    }

    override suspend fun getSlowMovingItems(daysThreshold: Int): List<SlowMovingItem> {
        val allBooks = catalogDao.getAllBooks()
        val now = System.currentTimeMillis()

        return allBooks
            .filter { it.currentStock > 0 }
            .mapNotNull { stockItem ->
                val lastSaleDate = billingDao.getLastSaleDateForBook(stockItem.book.id)
                val daysSinceLastSale = if (lastSaleDate != null) {
                    ((now - lastSaleDate) / 86400000).toInt()
                } else Int.MAX_VALUE

                if (daysSinceLastSale > daysThreshold) {
                    SlowMovingItem(
                        bookId = stockItem.book.id,
                        title = stockItem.book.titleBn,
                        currentStock = stockItem.currentStock,
                        lastSaleDate = lastSaleDate,
                        daysSinceLastSale = daysSinceLastSale
                    )
                } else null
            }
            .sortedByDescending { it.daysSinceLastSale }
    }

    override suspend fun getFastMovingItems(): List<FastMovingItem> {
        val allBooks = catalogDao.getAllBooks()

        return allBooks
            .map { stockItem ->
                val avgMonthlySales = calculateAverageMonthlySales(stockItem.book.id)
                val suggestedReorderQuantity = maxOf(0, (avgMonthlySales * 2) - stockItem.currentStock)

                FastMovingItem(
                    bookId = stockItem.book.id,
                    title = stockItem.book.titleBn,
                    currentStock = stockItem.currentStock,
                    avgMonthlySales = avgMonthlySales,
                    suggestedReorderQuantity = suggestedReorderQuantity
                )
            }
            .filter { it.avgMonthlySales > 5 } // At least 5 sales per month
            .sortedByDescending { it.avgMonthlySales }
    }

    override suspend fun getTopCustomers(limit: Int): List<TopCustomer> {
        return khataDao.getTopCustomers(limit)
            .map { customer ->
                TopCustomer(
                    customerId = customer.customer.id,
                    name = customer.customer.nameBn,
                    totalPurchases = customer.totalPurchases,
                    totalPaid = customer.totalPaid,
                    dueAmount = customer.balance,
                    visitCount = customer.visitCount
                )
            }
    }

    override suspend fun getCustomersWithDue(): List<TopCustomer> {
        return khataDao.getCustomersWithDue()
            .filter { it.balance > 0 }
            .map { customer ->
                TopCustomer(
                    customerId = customer.customer.id,
                    name = customer.customer.nameBn,
                    totalPurchases = customer.totalPurchases,
                    totalPaid = customer.totalPaid,
                    dueAmount = customer.balance,
                    visitCount = customer.visitCount
                )
            }
    }

    override fun getDailySummariesFlow(): Flow<List<DailySummary>> = flow {
        // TODO: Implement as Flow with periodic updates
        emit(emptyList())
    }

    override suspend fun calculateProfitMargin(period: AnalyticsPeriod): Double {
        val analytics = getAnalyticsData(period)
        return analytics.profitMargin
    }

    override suspend fun calculateGrowthRate(currentPeriod: AnalyticsPeriod): Double? {
        val now = System.currentTimeMillis()
        val (currentStart, currentEnd) = getDateRange(currentPeriod)
        val (previousStart, previousEnd) = getPreviousDateRange(currentPeriod)

        val currentSales = billingDao.getTotalSalesBetween(currentStart, currentEnd)
        val previousSales = billingDao.getTotalSalesBetween(previousStart, previousEnd)

        return if (previousSales > 0) {
            ((currentSales - previousSales) / previousSales) * 100
        } else null
    }

    override suspend fun getTotalDueAmount(): Double {
        return khataDao.getTotalDueAmount()
    }

    override suspend fun getCashInHand(): Double {
        return accountingDao.getCurrentCashbookBalance()
    }

    override suspend fun exportAnalyticsCsv(period: AnalyticsPeriod): String {
        val analytics = getAnalyticsData(period)
        
        return buildString {
            appendLine("মেট্রিক,মান")
            appendLine("মোট বিক্রয়,${analytics.totalSales}")
            appendLine("মোট খরচ,${analytics.totalExpenses}")
            appendLine("নিট লাভ,${analytics.netProfit}")
            appendLine("লাভের হার,${analytics.profitMargin}%")
            appendLine("কাস্টমার সংখ্যা,${analytics.customerCount}")
            appendLine("বকেয়া আদায়,${analytics.dueAmount}")
            appendLine("হাতে ক্যাশ,${analytics.cashInHand}")
            appendLine()
            appendLine("সেরা পণ্য")
            analytics.topProducts.forEachIndexed { index, product ->
                appendLine("${index + 1},${product.titleBn},${product.quantitySold},${product.revenue}")
            }
        }
    }

    override fun clearCache() {
        // Clear any cached analytics data
    }

    // Helper methods
    private fun getDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (period) {
            AnalyticsPeriod.TODAY -> {
                val start = now - (now % 86400000)
                Pair(start, start + 86400000)
            }
            AnalyticsPeriod.YESTERDAY -> {
                val start = now - (now % 86400000) - 86400000
                Pair(start, start + 86400000)
            }
            AnalyticsPeriod.THIS_WEEK -> {
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val start = now - ((dayOfWeek - 1) * 86400000L) - (now % 86400000)
                Pair(start, start + (7 * 86400000L))
            }
            AnalyticsPeriod.LAST_WEEK -> {
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val start = now - ((dayOfWeek + 6) * 86400000L) - (now % 86400000)
                Pair(start, start + (7 * 86400000L))
            }
            AnalyticsPeriod.THIS_MONTH -> {
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                val start = now - ((calendar.get(java.util.Calendar.DAY_OF_MONTH) - 1) * 86400000L) - (now % 86400000)
                Pair(start, now)
            }
            AnalyticsPeriod.LAST_MONTH -> {
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                val start = now - (calendar.get(java.util.Calendar.DAY_OF_MONTH) * 86400000L) - (now % 86400000)
                val end = now - ((calendar.get(java.util.Calendar.DAY_OF_MONTH) - 1) * 86400000L) - (now % 86400000)
                Pair(start, end)
            }
            AnalyticsPeriod.THIS_YEAR -> {
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                val start = now - ((calendar.get(java.util.Calendar.DAY_OF_YEAR) - 1) * 86400000L) - (now % 86400000)
                Pair(start, now)
            }
            AnalyticsPeriod.CUSTOM -> Pair(0L, now)
        }
    }

    private fun getPreviousDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val (start, end) = getDateRange(period)
        val duration = end - start
        return Pair(start - duration, start)
    }

    private fun calculateSalesTrendForRange(startDate: Long, endDate: Long): List<SalesTrendPoint> {
        // Simplified implementation
        return emptyList()
    }

    private fun calculateTopProductsForRange(startDate: Long, endDate: Long): List<TopProduct> {
        // Simplified implementation
        return emptyList()
    }

    private fun calculateExpenseBreakdownForRange(startDate: Long, endDate: Long): Map<String, Double> {
        // Simplified implementation
        return emptyMap()
    }

    private fun calculateAverageMonthlySales(bookId: String): Int {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 86400000L)
        val totalQuantity = billingDao.getTotalQuantitySoldBetween(thirtyDaysAgo, now, bookId)
        return totalQuantity
    }

    private fun getDayName(dayIndex: Int): String {
        val days = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
        return days[dayIndex]
    }

    private fun getMonthName(monthIndex: Int): String {
        val months = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", 
                          "জুলাই", "আগস্ট", "সেপ্ট", "অক্টো", "নভে", "ডিসে")
        return months[monthIndex]
    }

    private fun getStartOfYear(year: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfYear(year: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(year, java.util.Calendar.DECEMBER, 31, 23, 59, 59)
        }
        return calendar.timeInMillis
    }
}
