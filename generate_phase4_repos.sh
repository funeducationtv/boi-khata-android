#!/bin/bash
mkdir -p app/src/main/java/com/boikhata/domain/repository
cat << 'INNER_EOF' > app/src/main/java/com/boikhata/domain/repository/KhataRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.KhataCustomer
import com.boikhata.domain.model.KhataCustomerWithBalance
import com.boikhata.domain.model.KhataEntry
import com.boikhata.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface KhataRepository {
    fun getCustomersWithBalance(): Flow<List<KhataCustomerWithBalance>>
    fun getEntriesForCustomer(customerId: String): Flow<List<KhataEntry>>
    suspend fun createCustomer(nameBn: String, phone: String, address: String, creditLimit: Double, userId: String): Result<KhataCustomer>
    suspend fun recordPayment(customerId: String, amount: Double, method: PaymentMethod, userId: String): Result<KhataEntry>
    suspend fun addManualCredit(customerId: String, amount: Double, description: String, userId: String): Result<KhataEntry>
}
INNER_EOF

cat << 'INNER_EOF' > app/src/main/java/com/boikhata/domain/repository/AccountingRepository.kt
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
INNER_EOF

cat << 'INNER_EOF' > app/src/main/java/com/boikhata/domain/repository/MasterCatalogRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.MasterCatalogBook
import kotlinx.coroutines.flow.Flow

interface MasterCatalogRepository {
    fun getAllMasterBooks(): Flow<List<MasterCatalogBook>>
    suspend fun seedInitialData(): Result<Unit>
    suspend fun importBookToCatalog(masterBookId: String, purchasePrice: Double, initialStock: Int, userId: String): Result<Unit>
}
INNER_EOF
