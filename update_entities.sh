#!/bin/bash
sed -i '/@Entity(tableName = "khata_entries")/,/val referenceId: String?/d' app/src/main/java/com/boikhata/domain/model/Phase3Entities.kt

cat << 'INNER_EOF' >> app/src/main/java/com/boikhata/domain/model/Phase3Entities.kt

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

INNER_EOF
