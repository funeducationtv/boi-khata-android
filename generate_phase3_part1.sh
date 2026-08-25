#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 1. ENUMS FOR PHASE 3
cat << 'INNER_EOF' > $PKG_DIR/domain/model/Phase3Enums.kt
package com.boikhata.domain.model

enum class BookCategory { NCTB, GENERAL, STATIONERY, OTHER }
enum class StockChangeReason { PURCHASE, SALE, RETURN, DAMAGE, ADJUSTMENT }
enum class PaymentMethod { CASH, BKASH, NAGAD, ROCKET, CARD, CREDIT }
enum class DiscountType { NONE, PERCENT, FIXED }
enum class SyncStatus { PENDING, SYNCED, CONFLICT }
enum class KhataEntryType { DUE, PAYMENT }
INNER_EOF

# 2. ENTITIES FOR PHASE 3
cat << 'INNER_EOF' > $PKG_DIR/domain/model/Phase3Entities.kt
package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val tenantId: String,
    val isbn: String?,
    val titleBn: String,
    val titleEn: String?,
    val author: String,
    val publisher: String,
    val classLevel: String,
    val subject: String,
    val editionYear: String,
    val category: BookCategory,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val initialStock: Int, // Captured once, not updated directly
    val lowStockThreshold: Int = 5,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "stock_ledger")
data class StockLedgerEntry(
    @PrimaryKey val id: String,
    val tenantId: String,
    val bookId: String,
    val changeQuantity: Int,
    val reason: StockChangeReason,
    val referenceId: String?,
    val userId: String,
    val timestamp: Long,
    val idempotencyKey: String
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey val id: String,
    val tenantId: String,
    val billNumber: String,
    val customerId: String?,
    val customerNameBn: String,
    val customerPhone: String,
    val userId: String,
    val subtotal: Double,
    val discountAmount: Double,
    val discountType: DiscountType,
    val vatAmount: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val paidAmount: Double,
    val dueAmount: Double,
    val khataEntryId: String?,
    val billDate: Long,
    val syncStatus: SyncStatus,
    val idempotencyKey: String
)

@Entity(tableName = "bill_lines")
data class BillLine(
    @PrimaryKey val id: String,
    val tenantId: String,
    val billId: String,
    val bookId: String,
    val bookTitleBn: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val vatAmount: Double
)

@Entity(tableName = "khata_entries")
data class KhataEntry(
    @PrimaryKey val id: String,
    val tenantId: String,
    val customerId: String?,
    val amount: Double,
    val type: KhataEntryType,
    val date: Long,
    val referenceId: String?
)

data class BookWithStock(
    val book: Book,
    val currentStock: Int
)

data class BillWithLines(
    val bill: Bill,
    val lines: List<BillLine>
)
INNER_EOF

# 3. UTILITIES
mkdir -p $PKG_DIR/util
cat << 'INNER_EOF' > $PKG_DIR/util/BengaliUtils.kt
package com.boikhata.util

object BengaliUtils {
    private val engToBnMap = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    
    fun Int.toBn(): String = this.toString().map { engToBnMap[it] ?: it }.joinToString("")
    fun Double.toBn(): String = String.format("%.2f", this).map { engToBnMap[it] ?: it }.joinToString("")
    fun Long.toBn(): String = this.toString().map { engToBnMap[it] ?: it }.joinToString("")
    fun String.toBn(): String = this.map { engToBnMap[it] ?: it }.joinToString("")
    
    fun formatCurrency(amount: Double): String = "৳" + amount.toBn()
    fun formatCurrency(amount: Int): String = "৳" + amount.toBn()
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/util/VatCalculator.kt
package com.boikhata.util

import com.boikhata.domain.model.BookCategory

object VatCalculator {
    fun calculateVat(category: BookCategory, price: Double, quantity: Int): Double {
        val vatRate = when (category) {
            BookCategory.STATIONERY -> 0.15
            else -> 0.0
        }
        return (price * quantity) * vatRate
    }
}
INNER_EOF

