#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/boikhata/data/repository/BillingRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.BillingDao
import com.boikhata.data.local.KhataDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val billingDao: BillingDao,
    private val khataDao: KhataDao
) : BillingRepository {
    private val tenantId = "t_1"

    override fun getAllBills(): Flow<List<Bill>> {
        return billingDao.getAllBills(tenantId)
    }

    override suspend fun getBillById(billId: String): BillWithLines? {
        val bill = billingDao.getBillById(billId, tenantId) ?: return null
        val lines = billingDao.getBillLines(billId, tenantId)
        return BillWithLines(bill, lines)
    }

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

            var khataEntryId: String? = null
            var customerId: String? = null
            
            // If there's due amount, try to auto-create or find customer and create khata entry
            if (dueAmount > 0) {
                // Find or create customer
                val existingCustomer = khataDao.getCustomerByPhone(tenantId, customerPhone)
                if (existingCustomer != null) {
                    customerId = existingCustomer.id
                } else {
                    customerId = UUID.randomUUID().toString()
                    khataDao.insertCustomer(KhataCustomer(
                        id = customerId,
                        tenantId = tenantId,
                        nameBn = customerName,
                        phone = customerPhone,
                        address = "",
                        creditLimit = 5000.0,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    ))
                }
                
                khataEntryId = UUID.randomUUID().toString()
                khataDao.insertEntry(KhataEntry(
                    id = khataEntryId,
                    tenantId = tenantId,
                    customerId = customerId,
                    amount = dueAmount,
                    type = KhataEntryType.CREDIT,
                    description = "Bill $billId",
                    referenceBillId = billId,
                    collectedByUserId = userId,
                    date = now,
                    idempotencyKey = idempotency
                ))
            }

            val bill = Bill(
                id = billId,
                tenantId = tenantId,
                billNumber = "B-${System.currentTimeMillis() % 10000}",
                customerId = customerId,
                customerNameBn = customerName,
                customerPhone = customerPhone,
                userId = userId,
                subtotal = subtotal,
                discountAmount = actualDiscount,
                discountType = discountType,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod,
                paidAmount = paidAmount,
                dueAmount = dueAmount,
                khataEntryId = khataEntryId,
                billDate = now,
                syncStatus = SyncStatus.PENDING,
                idempotencyKey = idempotency
            )
            
            billingDao.insertBill(bill)
            
            val newLines = lines.map {
                it.copy(id = UUID.randomUUID().toString(), tenantId = tenantId, billId = billId)
            }
            billingDao.insertBillLines(newLines)
            
            return Result.success(bill)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
INNER_EOF
