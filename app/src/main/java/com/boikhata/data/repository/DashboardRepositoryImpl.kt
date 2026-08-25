package com.boikhata.data.repository

import com.boikhata.data.local.AccountingDao
import com.boikhata.data.local.BillingDao
import com.boikhata.data.local.CatalogDao
import com.boikhata.data.local.KhataDao
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val billingDao: BillingDao,
    private val khataDao: KhataDao,
    private val accountingDao: AccountingDao,
    private val catalogDao: CatalogDao
) : DashboardRepository {
    private val tenantId = "t_1"

    override fun getDashboardData(): Flow<DashboardData?> {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
        
        return combine(
            billingDao.getAllBills(tenantId),
            khataDao.getCustomersWithBalance(tenantId),
            accountingDao.getExpenses(tenantId),
            catalogDao.getBooksWithStock(tenantId)
        ) { bills, khataCustomers, expenses, catalog ->
            
            val todayBills = bills.filter { it.billDate >= todayStart }
            val todaySales = todayBills.sumOf { it.totalAmount }
            val todayExpenses = expenses.filter { it.expense.expenseDate >= todayStart }.sumOf { it.expense.amount }
            
            val todayProfit = todaySales - todayExpenses
            
            val lowStockCount = catalog.count { it.currentStock <= it.book.lowStockThreshold }
            
            val collectToday = khataCustomers
                .filter { it.balance > 0 }
                .sortedByDescending { it.balance }
                .take(3)
                .map { CollectItem(it.customer.id, it.customer.nameBn, it.balance.toInt(), it.daysOverdue, it.daysOverdue > 30) }

            DashboardData.Owner(
                tenantName = "Boi Khata",
                licenseState = LicenseState.ACTIVE,
                licenseDaysRemaining = 365,
                todaySales = todaySales.toInt(),
                todayCollection = 0, // Simplified for now
                todayProfit = todayProfit.toInt(),
                lowStockCount = lowStockCount,
                collectTodayList = collectToday
            )
        }
    }
}
