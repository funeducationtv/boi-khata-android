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
    suspend fun insertBooks(books: List<Book>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockLedgerEntry(entry: StockLedgerEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockLedgerEntries(entries: List<StockLedgerEntry>)

    @Transaction
    @Query("""
        SELECT b.*, 
        (b.initialStock + COALESCE((SELECT SUM(changeQuantity) FROM stock_ledger WHERE bookId = b.id), 0)) as currentStock 
        FROM books b 
        WHERE b.tenantId = :tenantId AND b.isActive = 1
    """)
    fun getBooksWithStock(tenantId: String): Flow<List<BookWithStock>>

    @Query("SELECT * FROM books WHERE tenantId = :tenantId")
    suspend fun getAllBooksDirect(tenantId: String): List<Book>

    @Query("SELECT * FROM stock_ledger WHERE tenantId = :tenantId")
    suspend fun getAllStockEntriesDirect(tenantId: String): List<StockLedgerEntry>
    
    @Query("SELECT * FROM books WHERE id = :id AND tenantId = :tenantId")
    suspend fun getBookById(tenantId: String, id: String): Book?
}

@Dao
interface BillingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<Bill>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillLines(lines: List<BillLine>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntry)

    @Query("SELECT * FROM bills WHERE tenantId = :tenantId ORDER BY billDate DESC")
    fun getAllBills(tenantId: String): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE tenantId = :tenantId")
    suspend fun getAllBillsDirect(tenantId: String): List<Bill>

    @Query("SELECT * FROM bill_lines WHERE tenantId = :tenantId")
    suspend fun getAllBillLinesDirect(tenantId: String): List<BillLine>

    @Query("SELECT * FROM bills WHERE id = :billId AND tenantId = :tenantId")
    suspend fun getBillById(tenantId: String, billId: String): Bill?

    @Query("SELECT * FROM bill_lines WHERE billId = :billId AND tenantId = :tenantId")
    suspend fun getLinesForBill(tenantId: String, billId: String): List<BillLine>
    
    @Query("UPDATE bills SET syncStatus = :status WHERE id = :billId")
    suspend fun updateBillStatus(billId: String, status: SyncStatus)
}
