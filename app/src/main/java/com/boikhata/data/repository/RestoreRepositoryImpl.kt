package com.boikhata.data.repository

import com.boikhata.data.local.*
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.RestoreProgress
import com.boikhata.domain.repository.RestoreRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreRepositoryImpl @Inject constructor(
    private val catalogDao: CatalogDao,
    private val billingDao: BillingDao,
    private val khataDao: KhataDao,
    private val accountingDao: AccountingDao,
    private val cloudSyncDao: CloudSyncDao
) : RestoreRepository {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override suspend fun checkCloudDataAvailable(tenantId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (tenantId.isBlank()) return@withContext Result.success(false)
        try {
            val billsSnap = firestore.collection("bills")
                .whereEqualTo("tenantId", tenantId)
                .limit(1).get().await()
            if (!billsSnap.isEmpty) return@withContext Result.success(true)

            val booksSnap = firestore.collection("books")
                .whereEqualTo("tenantId", tenantId)
                .limit(1).get().await()
            if (!booksSnap.isEmpty) return@withContext Result.success(true)

            val custSnap = firestore.collection("khata_customers")
                .whereEqualTo("tenantId", tenantId)
                .limit(1).get().await()
            Result.success(!custSnap.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun performRestore(tenantId: String): Flow<RestoreProgress> = flow {
        if (tenantId.isBlank()) {
            emit(RestoreProgress.Error("দোকানের আইডি (Tenant ID) পাওয়া যায়নি।"))
            return@flow
        }

        try {
            // All queries are tenant-scoped and read the SAME top-level paths the backup writes.
            // Mapping is explicit field-by-field (never toObjects() on these data classes).

            emit(RestoreProgress.Restoring("বই ও স্টক নামানো হচ্ছে...", 0.15f))
            val booksSnap = firestore.collection("books").whereEqualTo("tenantId", tenantId).get().await()
            val books = booksSnap.documents.mapNotNull(::bookFromDoc)
            if (books.isNotEmpty()) catalogDao.insertBooks(books)

            val stockSnap = firestore.collection("stock_ledger").whereEqualTo("tenantId", tenantId).get().await()
            val stockEntries = stockSnap.documents.mapNotNull(::stockFromDoc)
            if (stockEntries.isNotEmpty()) catalogDao.insertStockLedgerEntries(stockEntries)

            emit(RestoreProgress.Restoring("বিল ও বিক্রির হিসাব নামানো হচ্ছে...", 0.4f))
            val billsSnap = firestore.collection("bills").whereEqualTo("tenantId", tenantId).get().await()
            val bills = billsSnap.documents.mapNotNull(::billFromDoc)
            if (bills.isNotEmpty()) billingDao.insertBills(bills)

            val billLinesSnap = firestore.collection("bill_lines").whereEqualTo("tenantId", tenantId).get().await()
            val billLines = billLinesSnap.documents.mapNotNull(::billLineFromDoc)
            if (billLines.isNotEmpty()) billingDao.insertBillLines(billLines)

            emit(RestoreProgress.Restoring("খাতার কাস্টমার ও বাকি হিসাব নামানো হচ্ছে...", 0.65f))
            val custSnap = firestore.collection("khata_customers").whereEqualTo("tenantId", tenantId).get().await()
            val customers = custSnap.documents.mapNotNull(::customerFromDoc)
            if (customers.isNotEmpty()) khataDao.insertCustomers(customers)

            val khataEntriesSnap = firestore.collection("khata_entries").whereEqualTo("tenantId", tenantId).get().await()
            val khataEntries = khataEntriesSnap.documents.mapNotNull(::khataEntryFromDoc)
            if (khataEntries.isNotEmpty()) khataDao.insertEntries(khataEntries)

            emit(RestoreProgress.Restoring("খরচ ও ক্যাশ বুক নামানো হচ্ছে...", 0.85f))

            // expense_categories MUST be restored before expenses (relation integrity).
            val catSnap = firestore.collection("expense_categories").whereEqualTo("tenantId", tenantId).get().await()
            val categories = catSnap.documents.mapNotNull(::categoryFromDoc)
            if (categories.isNotEmpty()) accountingDao.insertExpenseCategories(categories)

            val expSnap = firestore.collection("expenses").whereEqualTo("tenantId", tenantId).get().await()
            val expenses = expSnap.documents.mapNotNull(::expenseFromDoc)
            if (expenses.isNotEmpty()) accountingDao.insertExpenses(expenses)

            val cbSnap = firestore.collection("cashbook_entries").whereEqualTo("tenantId", tenantId).get().await()
            val cashbookEntries = cbSnap.documents.mapNotNull(::cashbookFromDoc)
            if (cashbookEntries.isNotEmpty()) accountingDao.insertCashbookEntries(cashbookEntries)

            val drawingSnap = firestore.collection("owner_drawings").whereEqualTo("tenantId", tenantId).get().await()
            val drawings = drawingSnap.documents.mapNotNull(::drawingFromDoc)
            if (drawings.isNotEmpty()) accountingDao.insertOwnerDrawings(drawings)

            cloudSyncDao.updateLastRestoreAt(System.currentTimeMillis())
            emit(RestoreProgress.Success("ক্লাউড থেকে সমস্ত ডাটা সফলভাবে রিস্টোর হয়েছে!"))
        } catch (e: Exception) {
            emit(RestoreProgress.Error("ডাটা রিস্টোরে ত্রুটি: ${e.localizedMessage ?: "ইন্টারনেট সংযোগ চেক করুন"}"))
        }
    }.flowOn(Dispatchers.IO)

    // --- Explicit field-by-field reconstruction (no reflection / no-arg constructors) ---

    private fun bookFromDoc(d: DocumentSnapshot): Book? {
        val titleBn = d.getString("titleBn") ?: return null
        return Book(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            isbn = d.getString("isbn")?.takeIf { it.isNotEmpty() },
            titleBn = titleBn,
            titleEn = d.getString("titleEn")?.takeIf { it.isNotEmpty() },
            author = d.getString("author") ?: "",
            publisher = d.getString("publisher") ?: "",
            classLevel = d.getString("classLevel") ?: "",
            subject = d.getString("subject") ?: "",
            editionYear = d.getString("editionYear") ?: "",
            category = enumOrDefault(d.getString("category"), BookCategory.GENERAL),
            purchasePrice = d.getDouble("purchasePrice") ?: 0.0,
            sellingPrice = d.getDouble("sellingPrice") ?: 0.0,
            initialStock = (d.getLong("initialStock") ?: 0L).toInt(),
            lowStockThreshold = (d.getLong("lowStockThreshold") ?: 5L).toInt(),
            isActive = d.getBoolean("isActive") ?: true,
            createdAt = d.getLong("createdAt") ?: 0L,
            updatedAt = d.getLong("updatedAt") ?: 0L
        )
    }

    private fun stockFromDoc(d: DocumentSnapshot): StockLedgerEntry? {
        val bookId = d.getString("bookId") ?: return null
        return StockLedgerEntry(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            bookId = bookId,
            changeQuantity = (d.getLong("changeQuantity") ?: 0L).toInt(),
            reason = enumOrDefault(d.getString("reason"), StockChangeReason.ADJUSTMENT),
            referenceId = d.getString("referenceId")?.takeIf { it.isNotEmpty() },
            userId = d.getString("userId") ?: "",
            timestamp = d.getLong("timestamp") ?: 0L,
            idempotencyKey = d.getString("idempotencyKey") ?: d.id
        )
    }

    private fun billFromDoc(d: DocumentSnapshot): Bill? {
        val billNumber = d.getString("billNumber") ?: return null
        return Bill(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            billNumber = billNumber,
            customerId = d.getString("customerId")?.takeIf { it.isNotEmpty() },
            customerNameBn = d.getString("customerNameBn") ?: "",
            customerPhone = d.getString("customerPhone") ?: "",
            userId = d.getString("userId") ?: "",
            subtotal = d.getDouble("subtotal") ?: 0.0,
            discountAmount = d.getDouble("discountAmount") ?: 0.0,
            discountType = enumOrDefault(d.getString("discountType"), DiscountType.NONE),
            vatAmount = d.getDouble("vatAmount") ?: 0.0,
            totalAmount = d.getDouble("totalAmount") ?: 0.0,
            paymentMethod = enumOrDefault(d.getString("paymentMethod"), PaymentMethod.CASH),
            paidAmount = d.getDouble("paidAmount") ?: 0.0,
            dueAmount = d.getDouble("dueAmount") ?: 0.0,
            khataEntryId = d.getString("khataEntryId")?.takeIf { it.isNotEmpty() },
            billDate = d.getLong("billDate") ?: 0L,
            syncStatus = enumOrDefault(d.getString("syncStatus"), SyncStatus.PENDING),
            idempotencyKey = d.getString("idempotencyKey") ?: d.id
        )
    }

    private fun billLineFromDoc(d: DocumentSnapshot): BillLine? {
        val billId = d.getString("billId") ?: return null
        return BillLine(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            billId = billId,
            bookId = d.getString("bookId") ?: "",
            bookTitleBn = d.getString("bookTitleBn") ?: "",
            quantity = (d.getLong("quantity") ?: 0L).toInt(),
            unitPrice = d.getDouble("unitPrice") ?: 0.0,
            lineTotal = d.getDouble("lineTotal") ?: 0.0,
            vatAmount = d.getDouble("vatAmount") ?: 0.0
        )
    }

    private fun customerFromDoc(d: DocumentSnapshot): KhataCustomer? {
        val nameBn = d.getString("nameBn") ?: return null
        return KhataCustomer(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            nameBn = nameBn,
            phone = d.getString("phone") ?: "",
            address = d.getString("address") ?: "",
            creditLimit = d.getDouble("creditLimit") ?: 5000.0,
            isActive = d.getBoolean("isActive") ?: true,
            createdAt = d.getLong("createdAt") ?: 0L,
            updatedAt = d.getLong("updatedAt") ?: 0L
        )
    }

    private fun khataEntryFromDoc(d: DocumentSnapshot): KhataEntry? {
        val customerId = d.getString("customerId") ?: return null
        // Backup flips negative ADJUSTMENT amounts to magnitude + "Negative Adj: " prefix
        // (cloud rules require amount > 0 on this append-only collection). Restore must
        // reverse the flip so the signed amount and original description survive intact.
        var amount = d.getDouble("amount") ?: 0.0
        var description = d.getString("description") ?: ""
        val type = enumOrDefault(d.getString("type"), KhataEntryType.CREDIT)
        if (type == KhataEntryType.ADJUSTMENT && description.startsWith("Negative Adj: ")) {
            description = description.removePrefix("Negative Adj: ")
            amount = -amount
        }
        return KhataEntry(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            customerId = customerId,
            amount = amount,
            type = type,
            description = description,
            referenceBillId = d.getString("referenceBillId")?.takeIf { it.isNotEmpty() },
            collectedByUserId = d.getString("collectedByUserId") ?: "",
            date = d.getLong("date") ?: 0L,
            idempotencyKey = d.getString("idempotencyKey") ?: d.id
        )
    }

    private fun categoryFromDoc(d: DocumentSnapshot): ExpenseCategory? {
        val nameBn = d.getString("nameBn") ?: return null
        return ExpenseCategory(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            nameBn = nameBn,
            icon = d.getString("icon") ?: "",
            isActive = d.getBoolean("isActive") ?: true
        )
    }

    private fun expenseFromDoc(d: DocumentSnapshot): Expense? {
        val categoryId = d.getString("categoryId") ?: return null
        return Expense(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            categoryId = categoryId,
            amount = d.getDouble("amount") ?: 0.0,
            description = d.getString("description") ?: "",
            expenseDate = d.getLong("expenseDate") ?: 0L,
            receiptPhotoPath = d.getString("receiptPhotoPath")?.takeIf { it.isNotEmpty() },
            userId = d.getString("userId") ?: "",
            idempotencyKey = d.getString("idempotencyKey") ?: d.id
        )
    }

    private fun cashbookFromDoc(d: DocumentSnapshot): CashbookEntry? {
        val account = d.getString("account") ?: return null
        // Same negative-ADJUSTMENT reversal as khata entries (see khataEntryFromDoc).
        var amount = d.getDouble("amount") ?: 0.0
        var description = d.getString("description") ?: ""
        val type = enumOrDefault(d.getString("type"), CashbookEntryType.INCOME)
        if (type == CashbookEntryType.ADJUSTMENT && description.startsWith("Negative Adj: ")) {
            description = description.removePrefix("Negative Adj: ")
            amount = -amount
        }
        return CashbookEntry(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            account = enumOrDefault(account, CashbookAccount.CASH),
            type = type,
            amount = amount,
            description = description,
            referenceId = d.getString("referenceId")?.takeIf { it.isNotEmpty() },
            date = d.getLong("date") ?: 0L,
            userId = d.getString("userId") ?: "",
            idempotencyKey = d.getString("idempotencyKey") ?: d.id
        )
    }

    private fun drawingFromDoc(d: DocumentSnapshot): OwnerDrawing? {
        return OwnerDrawing(
            id = d.id,
            tenantId = d.getString("tenantId") ?: "",
            amount = d.getDouble("amount") ?: 0.0,
            description = d.getString("description") ?: "",
            drawingDate = d.getLong("drawingDate") ?: 0L,
            userId = d.getString("userId") ?: "",
            idempotencyKey = d.getString("idempotencyKey") ?: d.id
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrDefault(default) } ?: default
}