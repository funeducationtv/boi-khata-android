package com.boikhata.data.repository

import com.boikhata.data.local.AccountingDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AccountingRepository
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.ProfitAndLossData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

class AccountingRepositoryImpl @Inject constructor(
    private val accountingDao: AccountingDao,
    private val auditRepo: AuditRepository
) : AccountingRepository {
    private val tenantId = "t_1"

    override fun getExpenseCategories(): Flow<List<ExpenseCategory>> = accountingDao.getExpenseCategories(tenantId)
    override fun getExpenses(): Flow<List<ExpenseWithCategory>> = accountingDao.getExpenses(tenantId)
    override fun getCashbookEntries(account: CashbookAccount): Flow<List<CashbookEntry>> = accountingDao.getCashbookEntries(tenantId, account)
    override fun getOwnerDrawings(): Flow<List<OwnerDrawing>> = accountingDao.getOwnerDrawings(tenantId)

    override suspend fun createExpenseCategory(nameBn: String, icon: String): Result<ExpenseCategory> {
        return try {
            val cat = ExpenseCategory(UUID.randomUUID().toString(), tenantId, nameBn, icon)
            accountingDao.insertExpenseCategory(cat)
            Result.success(cat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addExpense(categoryId: String, amount: Double, description: String, photoPath: String?, userId: String): Result<Expense> {
        return try {
            val exp = Expense(
                id = UUID.randomUUID().toString(), tenantId = tenantId, categoryId = categoryId, amount = amount,
                description = description, expenseDate = System.currentTimeMillis(), receiptPhotoPath = photoPath,
                userId = userId, idempotencyKey = UUID.randomUUID().toString()
            )
            accountingDao.insertExpense(exp)
            auditRepo.logAction(userId, AuditAction.EXPENSE_CREATED, "Created expense of $amount")
            Result.success(exp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addOwnerDrawing(amount: Double, description: String, userId: String): Result<OwnerDrawing> {
        return try {
            val drawing = OwnerDrawing(
                id = UUID.randomUUID().toString(), tenantId = tenantId, amount = amount, description = description,
                drawingDate = System.currentTimeMillis(), userId = userId, idempotencyKey = UUID.randomUUID().toString()
            )
            accountingDao.insertOwnerDrawing(drawing)
            auditRepo.logAction(userId, AuditAction.DRAWING_CREATED, "Owner drew $amount")
            Result.success(drawing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setOpeningBalance(cash: Double, bkash: Double, bank: Double, userId: String): Result<Unit> {
        return try {
            if (cash > 0) accountingDao.insertCashbookEntry(createOpening(CashbookAccount.CASH, cash, userId))
            if (bkash > 0) accountingDao.insertCashbookEntry(createOpening(CashbookAccount.BKASH, bkash, userId))
            if (bank > 0) accountingDao.insertCashbookEntry(createOpening(CashbookAccount.BANK, bank, userId))
            auditRepo.logAction(userId, AuditAction.OPENING_BALANCE_SET, "Set opening balances")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createOpening(account: CashbookAccount, amount: Double, userId: String) = CashbookEntry(
        id = UUID.randomUUID().toString(), tenantId = tenantId, account = account, type = CashbookEntryType.INCOME,
        amount = amount, description = "Opening Balance", referenceId = null, date = System.currentTimeMillis(),
        userId = userId, idempotencyKey = UUID.randomUUID().toString()
    )

    override fun getMonthlyProfitAndLoss(monthStart: Long, monthEnd: Long): Flow<ProfitAndLossData> {
        return combine(
            accountingDao.getTotalExpenses(tenantId, monthStart, monthEnd),
            accountingDao.getTotalDrawings(tenantId, monthStart, monthEnd)
        ) { expenses, drawings ->
            val exp = expenses ?: 0.0
            val drw = drawings ?: 0.0
            // Simplified totalRevenue for now (we'll rely on Dashboard for complete P&L)
            ProfitAndLossData(totalRevenue = 0.0, totalExpenses = exp, totalDrawings = drw, profit = 0.0 - exp)
        }
    }
}
