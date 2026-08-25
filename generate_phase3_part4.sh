#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 9. CATALOG REPOSITORY
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/CatalogRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.Book
import com.boikhata.domain.model.BookWithStock
import com.boikhata.domain.model.StockChangeReason
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun getBooksWithStock(): Flow<List<BookWithStock>>
    suspend fun addBook(book: Book, initialStock: Int, userId: String): Result<Unit>
    suspend fun adjustStock(bookId: String, changeQty: Int, reason: StockChangeReason, userId: String): Result<Unit>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/CatalogRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.CatalogDao
import com.boikhata.domain.model.Book
import com.boikhata.domain.model.BookWithStock
import com.boikhata.domain.model.StockChangeReason
import com.boikhata.domain.model.StockLedgerEntry
import com.boikhata.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val catalogDao: CatalogDao
) : CatalogRepository {
    private val tenantId = "t_1" // Hardcoded for single-tenant offline mode in Phase 3

    override fun getBooksWithStock(): Flow<List<BookWithStock>> = catalogDao.getBooksWithStock(tenantId)

    override suspend fun addBook(book: Book, initialStock: Int, userId: String): Result<Unit> {
        try {
            catalogDao.insertBook(book.copy(tenantId = tenantId, initialStock = initialStock))
            // No initial ledger entry needed as it's tracked in initialStock per blueprint to avoid double-counting
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun adjustStock(bookId: String, changeQty: Int, reason: StockChangeReason, userId: String): Result<Unit> {
        try {
            val entry = StockLedgerEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                bookId = bookId,
                changeQuantity = changeQty,
                reason = reason,
                referenceId = null,
                userId = userId,
                timestamp = System.currentTimeMillis(),
                idempotencyKey = UUID.randomUUID().toString()
            )
            catalogDao.insertStockLedgerEntry(entry)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
INNER_EOF

# 10. BILLING REPOSITORY
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/BillingRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.Bill
import com.boikhata.domain.model.BillLine
import com.boikhata.domain.model.BillWithLines
import com.boikhata.domain.model.DiscountType
import com.boikhata.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    fun getAllBills(): Flow<List<Bill>>
    suspend fun createBill(
        userId: String,
        customerName: String,
        customerPhone: String,
        lines: List<BillLine>,
        discountAmount: Double,
        discountType: DiscountType,
        paymentMethod: PaymentMethod,
        paidAmount: Double
    ): Result<Bill>
    suspend fun voidBill(billId: String, userId: String): Result<Unit>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/BillingRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.BillingDao
import com.boikhata.data.local.CatalogDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.BillingRepository
import com.boikhata.util.VatCalculator
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val billingDao: BillingDao,
    private val catalogDao: CatalogDao,
    private val auditRepo: AuditRepository
) : BillingRepository {
    private val tenantId = "t_1"

    override fun getAllBills(): Flow<List<Bill>> = billingDao.getAllBills(tenantId)

    override suspend fun createBill(
        userId: String, customerName: String, customerPhone: String,
        lines: List<BillLine>, discountAmount: Double, discountType: DiscountType,
        paymentMethod: PaymentMethod, paidAmount: Double
    ): Result<Bill> {
        try {
            val billId = UUID.randomUUID().toString()
            val idempotency = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            
            val subtotal = lines.sumOf { it.lineTotal }
            val vatAmount = lines.sumOf { it.vatAmount }
            val totalBeforeDiscount = subtotal + vatAmount
            
            val actualDiscount = when(discountType) {
                DiscountType.NONE -> 0.0
                DiscountType.FIXED -> discountAmount
                DiscountType.PERCENT -> (subtotal * discountAmount) / 100.0
            }
            val totalAmount = totalBeforeDiscount - actualDiscount
            val dueAmount = totalAmount - paidAmount

            var khataId: String? = null
            if (paymentMethod == PaymentMethod.CREDIT || dueAmount > 0) {
                khataId = UUID.randomUUID().toString()
                billingDao.insertKhataEntry(
                    KhataEntry(
                        id = khataId, tenantId = tenantId, customerId = null,
                        amount = dueAmount, type = KhataEntryType.DUE,
                        date = now, referenceId = billId
                    )
                )
            }

            val bill = Bill(
                id = billId, tenantId = tenantId, billNumber = "BK-2026-${now.toString().takeLast(6)}",
                customerId = null, customerNameBn = customerName.ifBlank { "কাস্টমার" }, customerPhone = customerPhone,
                userId = userId, subtotal = subtotal, discountAmount = actualDiscount, discountType = discountType,
                vatAmount = vatAmount, totalAmount = totalAmount, paymentMethod = paymentMethod,
                paidAmount = paidAmount, dueAmount = dueAmount, khataEntryId = khataId,
                billDate = now, syncStatus = SyncStatus.PENDING, idempotencyKey = idempotency
            )
            
            val linesToInsert = lines.map { it.copy(id = UUID.randomUUID().toString(), billId = billId, tenantId = tenantId) }
            
            billingDao.insertBill(bill)
            billingDao.insertBillLines(linesToInsert)
            
            linesToInsert.forEach { line ->
                catalogDao.insertStockLedgerEntry(
                    StockLedgerEntry(
                        id = UUID.randomUUID().toString(), tenantId = tenantId, bookId = line.bookId,
                        changeQuantity = -line.quantity, reason = StockChangeReason.SALE,
                        referenceId = billId, userId = userId, timestamp = now,
                        idempotencyKey = UUID.randomUUID().toString()
                    )
                )
            }
            return Result.success(bill)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun voidBill(billId: String, userId: String): Result<Unit> {
        return try {
            val lines = billingDao.getLinesForBill(tenantId, billId)
            lines.forEach { line ->
                catalogDao.insertStockLedgerEntry(
                    StockLedgerEntry(
                        id = UUID.randomUUID().toString(), tenantId = tenantId, bookId = line.bookId,
                        changeQuantity = line.quantity, reason = StockChangeReason.RETURN,
                        referenceId = billId, userId = userId, timestamp = System.currentTimeMillis(),
                        idempotencyKey = UUID.randomUUID().toString()
                    )
                )
            }
            billingDao.updateBillStatus(billId, SyncStatus.CONFLICT) // Voided
            auditRepo.logAction(userId, AuditAction.ROLE_SWITCH, "Voided bill $billId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
INNER_EOF

# 11. BINDINGS
cat << 'INNER_EOF' >> $PKG_DIR/di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class Phase3RepositoryModule {
    @Binds
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository
    @Binds
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository
}
INNER_EOF

