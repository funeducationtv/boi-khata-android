package com.boikhata.data.repository

import com.boikhata.data.local.AccountingDao
import com.boikhata.data.local.BillingDao
import com.boikhata.data.local.CatalogDao
import com.boikhata.data.local.KhataDao
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CloudLicenseRepository
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val billingDao: BillingDao,
    private val khataDao: KhataDao,
    private val accountingDao: AccountingDao,
    private val catalogDao: CatalogDao,
    private val tenantIdProvider: TenantIdProvider,
    private val licenseRepo: CloudLicenseRepository
) : DashboardRepository {

    override fun getDashboardData(): Flow<DashboardData?> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { tid ->
            val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)

            combine(
                billingDao.getAllBills(tid),
                khataDao.getCustomersWithBalance(tid),
                accountingDao.getExpenses(tid),
                catalogDao.getBooksWithStock(tid),
                licenseRepo.observeLicenseState()
            ) { bills, khataCustomers, expenses, catalog, license ->

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

                // C1: license comes from the cloud sync state, never a hardcoded ACTIVE/365.
                // Pre-first-sync fallback is the Phase-1 seeded GRACE (14 days), never ACTIVE.
                DashboardData.Owner(
                    tenantName = "Boi Khata",
                    licenseState = license?.state ?: LicenseState.GRACE,
                    licenseDaysRemaining = license?.daysRemaining ?: 14,
                    todaySales = todaySales.toInt(),
                    todayCollection = 0, // Simplified for now
                    todayProfit = todayProfit.toInt(),
                    lowStockCount = lowStockCount,
                    collectTodayList = collectToday
                )
            }
        }
}
