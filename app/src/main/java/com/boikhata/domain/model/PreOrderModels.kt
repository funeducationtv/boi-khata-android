package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pre_orders")
data class PreOrder(
    @PrimaryKey val id: String,
    val tenantId: String,
    val customerId: String?,
    val bookId: String,
    val quantity: Int,
    val advanceAmount: Double,
    val expectedPrice: Double,
    val expectedDate: Long,
    val status: PreOrderStatus,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class PreOrderStatus {
    PENDING, ARRIVED, PARTIALLY_DELIVERED, COMPLETED, CANCELLED, REFUNDED
}

data class PreOrderWithDetails(
    val preOrder: PreOrder,
    val customer: KhataCustomer?,
    val book: Book
)
