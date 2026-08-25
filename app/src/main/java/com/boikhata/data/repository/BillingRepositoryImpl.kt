package com.boikhata.data.repository

import com.boikhata.data.local.BillingDao
import com.boikhata.data.local.CatalogDao
import com.boikhata.data.local.KhataDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val billingDao: BillingDao,
    private val catalogDao: CatalogDao,
    private val khataDao: KhataDao,
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
            var customerId: String? = null

            if (paymentMethod == PaymentMethod.CREDIT || dueAmount > 0) {
                val existingCustomer = khataDao.getCustomerByPhone(tenantId, customerPhone)
                if (existingCustomer != null) {
                    customerId = existingCustomer.id
                } else {
                    customerId = UUID.randomUUID().toString()
                    khataDao.insertCustomer(KhataCustomer(
                        id = customerId, tenantId = tenantId, nameBn = customerName.ifBlank { "কাস্টমার" },
                        phone = customerPhone, address = "", creditLimit = 5000.0,
                        isActive = true, createdAt = now, updatedAt = now
                    ))
                }

                khataId = UUID.randomUUID().toString()
                khataDao.insertEntry(
                    KhataEntry(
                        id = khataId, tenantId = tenantId, customerId = customerId,
                        amount = dueAmount, type = KhataEntryType.CREDIT,
                        description = "Bill $billId", referenceBillId = billId,
                        collectedByUserId = userId, date = now, idempotencyKey = idempotency
                    )
                )
            }

            val bill = Bill(
                id = billId, tenantId = tenantId, billNumber = "BK-2026-${now.toString().takeLast(6)}",
                customerId = customerId, customerNameBn = customerName.ifBlank { "কাস্টমার" }, customerPhone = customerPhone,
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
            billingDao.updateBillStatus(billId, SyncStatus.VOIDED) 
            auditRepo.logAction(userId, AuditAction.BILL_VOID, "Voided bill $billId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
