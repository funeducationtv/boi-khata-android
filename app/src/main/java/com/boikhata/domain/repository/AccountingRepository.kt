package com.boikhata.domain.repository

import com.boikhata.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AccountingRepository {
    fun getExpenseCategories(): Flow<List<ExpenseCategory>>
    fun getExpenses(): Flow<List<ExpenseWithCategory>>
    fun getCashbookEntries(account: CashbookAccount): Flow<List<CashbookEntry>>
    fun getOwnerDrawings(): Flow<List<OwnerDrawing>>
    
    suspend fun createExpenseCategory(nameBn: String, icon: String): Result<ExpenseCategory>
    suspend fun addExpense(categoryId: String, amount: Double, description: String, photoPath: String?, userId: String): Result<Expense>
    suspend fun addOwnerDrawing(amount: Double, description: String, userId: String): Result<OwnerDrawing>
    suspend fun setOpeningBalance(cash: Double, bkash: Double, bank: Double, userId: String): Result<Unit>
    
    fun getMonthlyProfitAndLoss(monthStart: Long, monthEnd: Long): Flow<ProfitAndLossData>
}

data class ProfitAndLossData(
    val totalRevenue: Double,
    val totalExpenses: Double,
    val totalDrawings: Double,
    val profit: Double
)
