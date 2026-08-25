#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/boikhata/data/repository/KhataRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.KhataDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.KhataRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class KhataRepositoryImpl @Inject constructor(
    private val khataDao: KhataDao,
    private val auditRepo: AuditRepository
) : KhataRepository {
    private val tenantId = "t_1"

    override fun getCustomersWithBalance(): Flow<List<KhataCustomerWithBalance>> = khataDao.getCustomersWithBalance(tenantId)
    override fun getEntriesForCustomer(customerId: String): Flow<List<KhataEntry>> = khataDao.getEntriesForCustomer(tenantId, customerId)

    override suspend fun createCustomer(nameBn: String, phone: String, address: String, creditLimit: Double, userId: String): Result<KhataCustomer> {
        return try {
            val customer = KhataCustomer(
                id = UUID.randomUUID().toString(), tenantId = tenantId, nameBn = nameBn, phone = phone,
                address = address, creditLimit = creditLimit, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            khataDao.insertCustomer(customer)
            auditRepo.logAction(userId, AuditAction.KHATA_CUSTOMER_CREATED, "Created khata customer ${customer.id}")
            Result.success(customer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPayment(customerId: String, amount: Double, method: PaymentMethod, userId: String): Result<KhataEntry> {
        return try {
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(), tenantId = tenantId, customerId = customerId, amount = amount,
                type = KhataEntryType.PAYMENT, description = "Collected via $method", referenceBillId = null,
                collectedByUserId = userId, date = System.currentTimeMillis(), idempotencyKey = UUID.randomUUID().toString()
            )
            khataDao.insertEntry(entry)
            auditRepo.logAction(userId, AuditAction.KHATA_PAYMENT_RECORDED, "Recorded payment of $amount for customer $customerId")
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addManualCredit(customerId: String, amount: Double, description: String, userId: String): Result<KhataEntry> {
        return try {
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(), tenantId = tenantId, customerId = customerId, amount = amount,
                type = KhataEntryType.CREDIT, description = description, referenceBillId = null,
                collectedByUserId = userId, date = System.currentTimeMillis(), idempotencyKey = UUID.randomUUID().toString()
            )
            khataDao.insertEntry(entry)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > app/src/main/java/com/boikhata/data/repository/AccountingRepositoryImpl.kt
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
INNER_EOF

cat << 'INNER_EOF' > app/src/main/java/com/boikhata/data/repository/MasterCatalogRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.MasterCatalogDao
import com.boikhata.data.local.CatalogDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.MasterCatalogRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class MasterCatalogRepositoryImpl @Inject constructor(
    private val masterDao: MasterCatalogDao,
    private val catalogDao: CatalogDao,
    private val auditRepo: AuditRepository
) : MasterCatalogRepository {
    private val tenantId = "t_1"

    override fun getAllMasterBooks(): Flow<List<MasterCatalogBook>> = masterDao.getAllMasterBooks()

    override suspend fun seedInitialData(): Result<Unit> {
        return try {
            val list = mutableListOf<MasterCatalogBook>()
            val classes = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 9", "Class 10")
            val subjects = listOf("Bangla", "English", "Math", "Science", "Social Science", "Religion", "ICT")
            
            var c = 1
            for (cls in classes) {
                for (sub in subjects) {
                    list.add(MasterCatalogBook(
                        id = UUID.randomUUID().toString(), isbn = "978-984-$c-000", titleBn = "NCTB $sub $cls",
                        titleEn = "$sub $cls", author = "NCTB", publisher = "NCTB", classLevel = cls, subject = sub,
                        editionYear = "2024", mrp = 150.0 + (c * 10), isActive = true, lastUpdated = System.currentTimeMillis()
                    ))
                    c++
                    if(c > 50) break
                }
                if(c > 50) break
            }
            masterDao.insertAll(list)
            Result.success(Unit)
        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importBookToCatalog(masterBookId: String, purchasePrice: Double, initialStock: Int, userId: String): Result<Unit> {
        return try {
            // Minimal mock of importing
            auditRepo.logAction(userId, AuditAction.CATALOG_IMPORTED, "Imported book $masterBookId")
            Result.success(Unit)
        } catch(e: Exception) {
            Result.failure(e)
        }
    }
}
INNER_EOF
