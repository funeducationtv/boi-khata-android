package com.boikhata.data.repository

import com.boikhata.data.local.AccountingDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AccountingRepository
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.ProfitAndLossData
import com.boikhata.security.LicenseWriteGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID
import javax.inject.Inject

class AccountingRepositoryImpl @Inject constructor(
    private val accountingDao: AccountingDao,
    private val tenantIdProvider: TenantIdProvider,
    private val writeGuard: LicenseWriteGuard,
    private val auditRepo: AuditRepository
) : AccountingRepository {

    override fun getExpenseCategories(): Flow<List<ExpenseCategory>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { accountingDao.getExpenseCategories(it) }

    override fun getExpenses(): Flow<List<ExpenseWithCategory>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { accountingDao.getExpenses(it) }

    override fun getCashbookEntries(account: CashbookAccount): Flow<List<CashbookEntry>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { accountingDao.getCashbookEntries(it, account) }

    override fun getOwnerDrawings(): Flow<List<OwnerDrawing>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { accountingDao.getOwnerDrawings(it) }

    override suspend fun createExpenseCategory(nameBn: String, icon: String): Result<ExpenseCategory> {
        return try {
            writeGuard.assertWritable()
            val cat = ExpenseCategory(UUID.randomUUID().toString(), tenantIdProvider.current(), nameBn, icon)
            accountingDao.insertExpenseCategory(cat)
            Result.success(cat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addExpense(categoryId: String, amount: Double, description: String, photoPath: String?, userId: String): Result<Expense> {
        return try {
            writeGuard.assertWritable()
            val exp = Expense(
                id = UUID.randomUUID().toString(), tenantId = tenantIdProvider.current(), categoryId = categoryId, amount = amount,
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
            writeGuard.assertWritable()
            val drawing = OwnerDrawing(
                id = UUID.randomUUID().toString(), tenantId = tenantIdProvider.current(), amount = amount, description = description,
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
            writeGuard.assertWritable()
            if (cash > 0) accountingDao.insertCashbookEntry(createOpening(CashbookAccount.CASH, cash, userId))
            if (bkash > 0) accountingDao.insertCashbookEntry(createOpening(CashbookAccount.BKASH, bkash, userId))
            if (bank > 0) accountingDao.insertCashbookEntry(createOpening(CashbookAccount.BANK, bank, userId))
            auditRepo.logAction(userId, AuditAction.OPENING_BALANCE_SET, "Set opening balances")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createOpening(account: CashbookAccount, amount: Double, userId: String) = CashbookEntry(
        id = UUID.randomUUID().toString(), tenantId = tenantIdProvider.current(), account = account, type = CashbookEntryType.INCOME,
        amount = amount, description = "Opening Balance", referenceId = null, date = System.currentTimeMillis(),
        userId = userId, idempotencyKey = UUID.randomUUID().toString()
    )

    override fun getMonthlyProfitAndLoss(monthStart: Long, monthEnd: Long): Flow<ProfitAndLossData> {
        return tenantIdProvider.tenantIdFlow().flatMapLatest { tid ->
            combine(
                accountingDao.getTotalExpenses(tid, monthStart, monthEnd),
                accountingDao.getTotalDrawings(tid, monthStart, monthEnd)
            ) { expenses, drawings ->
                val exp = expenses ?: 0.0
                val drw = drawings ?: 0.0
                // Simplified totalRevenue for now (we'll rely on Dashboard for complete P&L)
                ProfitAndLossData(totalRevenue = 0.0, totalExpenses = exp, totalDrawings = drw, profit = 0.0 - exp)
            }
        }
    }
}
