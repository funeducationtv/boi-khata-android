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
    val initialStock: Int,
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

data class BookWithStock(
    @androidx.room.Embedded val book: Book,
    val currentStock: Int
)

data class BillWithLines(
    val bill: Bill,
    val lines: List<BillLine>
)

@Entity(tableName = "khata_customers")
data class KhataCustomer(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val phone: String,
    val address: String,
    val creditLimit: Double = 5000.0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "khata_entries")
data class KhataEntry(
    @PrimaryKey val id: String,
    val tenantId: String,
    val customerId: String,
    val amount: Double,
    val type: KhataEntryType,
    val description: String,
    val referenceBillId: String?,
    val collectedByUserId: String,
    val date: Long,
    val idempotencyKey: String
)

@Entity(tableName = "expense_categories")
data class ExpenseCategory(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val icon: String,
    val isActive: Boolean = true
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val tenantId: String,
    val categoryId: String,
    val amount: Double,
    val description: String,
    val expenseDate: Long,
    val receiptPhotoPath: String?,
    val userId: String,
    val idempotencyKey: String
)

@Entity(tableName = "cashbook_entries")
data class CashbookEntry(
    @PrimaryKey val id: String,
    val tenantId: String,
    val account: CashbookAccount,
    val type: CashbookEntryType,
    val amount: Double,
    val description: String,
    val referenceId: String?,
    val date: Long,
    val userId: String,
    val idempotencyKey: String
)

@Entity(tableName = "owner_drawings")
data class OwnerDrawing(
    @PrimaryKey val id: String,
    val tenantId: String,
    val amount: Double,
    val description: String,
    val drawingDate: Long,
    val userId: String,
    val idempotencyKey: String
)

@Entity(tableName = "master_catalog_books")
data class MasterCatalogBook(
    @PrimaryKey val id: String,
    val isbn: String?,
    val titleBn: String,
    val titleEn: String?,
    val author: String,
    val publisher: String,
    val classLevel: String,
    val subject: String,
    val editionYear: String,
    val mrp: Double,
    val isActive: Boolean = true,
    val lastUpdated: Long
)

data class KhataCustomerWithBalance(
    @androidx.room.Embedded val customer: KhataCustomer,
    val balance: Double,
    val daysOverdue: Int
)

data class ExpenseWithCategory(
    @androidx.room.Embedded val expense: Expense,
    @androidx.room.Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: ExpenseCategory
)
