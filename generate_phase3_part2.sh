#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 4. DAOS FOR PHASE 3
cat << 'INNER_EOF' > $PKG_DIR/data/local/Phase3Daos.kt
package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.boikhata.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockLedgerEntry(entry: StockLedgerEntry)

    @Transaction
    @Query("""
        SELECT b.*, 
        (b.initialStock + COALESCE((SELECT SUM(changeQuantity) FROM stock_ledger WHERE bookId = b.id), 0)) as currentStock 
        FROM books b 
        WHERE b.tenantId = :tenantId AND b.isActive = 1
    """)
    fun getBooksWithStock(tenantId: String): Flow<List<BookWithStock>>
    
    @Query("SELECT * FROM books WHERE id = :id AND tenantId = :tenantId")
    suspend fun getBookById(tenantId: String, id: String): Book?
}

@Dao
interface BillingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillLines(lines: List<BillLine>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntry)

    @Query("SELECT * FROM bills WHERE tenantId = :tenantId ORDER BY billDate DESC")
    fun getAllBills(tenantId: String): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :billId AND tenantId = :tenantId")
    suspend fun getBillById(tenantId: String, billId: String): Bill?

    @Query("SELECT * FROM bill_lines WHERE billId = :billId AND tenantId = :tenantId")
    suspend fun getLinesForBill(tenantId: String, billId: String): List<BillLine>
    
    @Query("UPDATE bills SET syncStatus = :status WHERE id = :billId")
    suspend fun updateBillStatus(billId: String, status: SyncStatus)
}
INNER_EOF

# 5. UPDATE CONVERTERS
cat << 'INNER_EOF' > $PKG_DIR/data/local/Phase3Converters.kt
package com.boikhata.data.local

import androidx.room.TypeConverter
import com.boikhata.domain.model.*

class Phase3Converters {
    @TypeConverter fun fromBookCategory(v: BookCategory) = v.name
    @TypeConverter fun toBookCategory(n: String) = BookCategory.valueOf(n)
    
    @TypeConverter fun fromStockChangeReason(v: StockChangeReason) = v.name
    @TypeConverter fun toStockChangeReason(n: String) = StockChangeReason.valueOf(n)
    
    @TypeConverter fun fromPaymentMethod(v: PaymentMethod) = v.name
    @TypeConverter fun toPaymentMethod(n: String) = PaymentMethod.valueOf(n)
    
    @TypeConverter fun fromDiscountType(v: DiscountType) = v.name
    @TypeConverter fun toDiscountType(n: String) = DiscountType.valueOf(n)
    
    @TypeConverter fun fromSyncStatus(v: SyncStatus) = v.name
    @TypeConverter fun toSyncStatus(n: String) = SyncStatus.valueOf(n)
    
    @TypeConverter fun fromKhataEntryType(v: KhataEntryType) = v.name
    @TypeConverter fun toKhataEntryType(n: String) = KhataEntryType.valueOf(n)
}
INNER_EOF

# 6. UPDATE APPDATABASE
cat << 'INNER_EOF' > $PKG_DIR/data/local/AppDatabase.kt
package com.boikhata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boikhata.domain.model.*

@Database(
    entities = [
        Tenant::class, User::class, Device::class, LocalAuditLog::class,
        Book::class, StockLedgerEntry::class, Bill::class, BillLine::class, KhataEntry::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class, Phase3Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun catalogDao(): CatalogDao
    abstract fun billingDao(): BillingDao
}
INNER_EOF

