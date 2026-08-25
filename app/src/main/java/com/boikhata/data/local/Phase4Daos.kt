package com.boikhata.data.local

import androidx.room.*
import com.boikhata.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(tenantId: String, phone: String): KhataCustomer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: KhataCustomer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<KhataCustomer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: KhataEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<KhataEntry>)

    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND isActive = 1")
    fun getAllCustomers(tenantId: String): Flow<List<KhataCustomer>>

    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId")
    suspend fun getAllCustomersDirect(tenantId: String): List<KhataCustomer>

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId")
    suspend fun getAllKhataEntriesDirect(tenantId: String): List<KhataEntry>

    @Query("""
        SELECT c.*, 
        (SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE 0 END) - 
         SUM(CASE WHEN e.type = 'PAYMENT' THEN e.amount ELSE 0 END)) as balance,
        0 as daysOverdue
        FROM khata_customers c 
        LEFT JOIN khata_entries e ON c.id = e.customerId 
        WHERE c.tenantId = :tenantId AND c.isActive = 1
        GROUP BY c.id
    """)
    fun getCustomersWithBalance(tenantId: String): Flow<List<KhataCustomerWithBalance>>

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId AND customerId = :customerId ORDER BY date DESC")
    fun getEntriesForCustomer(tenantId: String, customerId: String): Flow<List<KhataEntry>>
}

@Dao
interface AccountingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashbookEntry(entry: CashbookEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashbookEntries(entries: List<CashbookEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerDrawing(drawing: OwnerDrawing)

    @Transaction
    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId ORDER BY expenseDate DESC")
    fun getExpenses(tenantId: String): Flow<List<ExpenseWithCategory>>

    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId")
    suspend fun getAllExpensesDirect(tenantId: String): List<Expense>

    @Query("SELECT * FROM expense_categories WHERE tenantId = :tenantId AND isActive = 1")
    fun getExpenseCategories(tenantId: String): Flow<List<ExpenseCategory>>

    @Query("SELECT * FROM cashbook_entries WHERE tenantId = :tenantId AND account = :account ORDER BY date DESC")
    fun getCashbookEntries(tenantId: String, account: CashbookAccount): Flow<List<CashbookEntry>>

    @Query("SELECT * FROM cashbook_entries WHERE tenantId = :tenantId")
    suspend fun getAllCashbookEntriesDirect(tenantId: String): List<CashbookEntry>

    @Query("SELECT * FROM owner_drawings WHERE tenantId = :tenantId ORDER BY drawingDate DESC")
    fun getOwnerDrawings(tenantId: String): Flow<List<OwnerDrawing>>
    
    @Query("SELECT SUM(amount) FROM expenses WHERE tenantId = :tenantId AND expenseDate >= :start AND expenseDate <= :end")
    fun getTotalExpenses(tenantId: String, start: Long, end: Long): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM owner_drawings WHERE tenantId = :tenantId AND drawingDate >= :start AND drawingDate <= :end")
    fun getTotalDrawings(tenantId: String, start: Long, end: Long): Flow<Double?>
}

@Dao
interface MasterCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<MasterCatalogBook>)

    @Query("SELECT * FROM master_catalog_books WHERE isActive = 1 ORDER BY classLevel, subject")
    fun getAllMasterBooks(): Flow<List<MasterCatalogBook>>

    @Query("SELECT * FROM master_catalog_books WHERE id = :id")
    suspend fun getMasterBookById(id: String): MasterCatalogBook?

    @Query("SELECT MAX(lastUpdated) FROM master_catalog_books")
    suspend fun getLatestMasterUpdateTimestamp(): Long?
}
