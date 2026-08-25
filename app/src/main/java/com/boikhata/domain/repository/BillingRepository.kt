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
