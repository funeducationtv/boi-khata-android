package com.boikhata.domain.repository

import com.boikhata.domain.model.LicenseState
import kotlinx.coroutines.flow.Flow

sealed interface DashboardData {
    data class Owner(
        val tenantName: String,
        val licenseState: LicenseState,
        val licenseDaysRemaining: Int,
        val todaySales: Int,
        val todayCollection: Int,
        val todayProfit: Int,
        val lowStockCount: Int,
        val collectTodayList: List<CollectItem>
    ) : DashboardData

    data class Manager(
        val tenantName: String,
        val todaySales: Int,
        val lowStockCount: Int,
        val todayProfit: Int
    ) : DashboardData

    data class Sales(
        val tenantName: String,
        val shiftSales: Int,
        val cashDrawer: Int
    ) : DashboardData

    data class Accountant(
        val tenantName: String,
        val expenseSummary: Int,
        val cashbookNogad: Int,
        val cashbookBkash: Int,
        val cashbookBank: Int,
        val vatStatus: String
    ) : DashboardData
}

data class CollectItem(
    val id: String,
    val name: String,
    val amount: Int,
    val daysOverdue: Int,
    val isCritical: Boolean
)

interface DashboardRepository {
    fun getDashboardData(): Flow<DashboardData?>
}
