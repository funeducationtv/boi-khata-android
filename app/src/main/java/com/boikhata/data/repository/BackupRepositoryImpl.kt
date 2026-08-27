package com.boikhata.data.repository

import com.boikhata.data.local.*
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.BackupProgress
import com.boikhata.domain.repository.BackupRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val catalogDao: CatalogDao,
    private val billingDao: BillingDao,
    private val khataDao: KhataDao,
    private val accountingDao: AccountingDao,
    private val cloudSyncDao: CloudSyncDao
) : BackupRepository {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private companion object {
        const val BATCH_LIMIT = 450
    }

    override fun getLastBackupTime(): Flow<Long?> {
        return cloudSyncDao.getCloudSyncState().map { it?.lastBackupAt }
    }

    override fun performBackup(tenantId: String): Flow<BackupProgress> = flow {
        if (tenantId.isBlank()) {
            emit(BackupProgress.Error("দোকানের আইডি (Tenant ID) পাওয়া যায়নি।"))
            return@flow
        }

        try {
            val lastBackupAt = cloudSyncDao.getSyncStateDirect()?.lastBackupAt ?: 0L
            val isFirstRun = lastBackupAt <= 0L

            // --- Load local rows, incrementally filtered (first run = full upload) ---
            emit(BackupProgress.BackingUp("বই ও স্টক প্রস্তুত হচ্ছে...", 0.1f))
            val books = catalogDao.getAllBooksDirect(tenantId)
                .filter { isFirstRun || it.updatedAt > lastBackupAt }
            val stockEntries = catalogDao.getAllStockEntriesDirect(tenantId)
                .filter { isFirstRun || it.timestamp > lastBackupAt }

            val bills = billingDao.getAllBillsDirect(tenantId)
                .filter { isFirstRun || it.billDate > lastBackupAt }
            val newBillIds = bills.map { it.id }.toSet()
            val billLines = billingDao.getAllBillLinesDirect(tenantId)
                .filter { isFirstRun || it.billId in newBillIds }

            emit(BackupProgress.BackingUp("খাতা ও কাস্টমার প্রস্তুত হচ্ছে...", 0.4f))
            val customers = khataDao.getAllCustomersDirect(tenantId)
                .filter { isFirstRun || it.updatedAt > lastBackupAt }
            val khataEntries = khataDao.getAllKhataEntriesDirect(tenantId)
                .filter { isFirstRun || it.date > lastBackupAt }

            val expenseCategories = accountingDao.getAllExpenseCategoriesDirect(tenantId) // no timestamp field -> full
            val expenses = accountingDao.getAllExpensesDirect(tenantId)
                .filter { isFirstRun || it.expenseDate > lastBackupAt }
            val cashbookEntries = accountingDao.getAllCashbookEntriesDirect(tenantId)
                .filter { isFirstRun || it.date > lastBackupAt }
            val ownerDrawings = accountingDao.getAllOwnerDrawingsDirect(tenantId)
                .filter { isFirstRun || it.drawingDate > lastBackupAt }

            emit(BackupProgress.BackingUp("ক্লাউডে ডাটা আপলোড হচ্ছে...", 0.7f))

            // Mutable collections: set() allowed (upsert-by-id).
            uploadCollection("books", books.map { it.id to bookToMap(it, tenantId) }, createOnly = false, tenantId)
            uploadCollection("bills", bills.map { it.id to billToMap(it, tenantId) }, createOnly = false, tenantId)
            uploadCollection("bill_lines", billLines.map { it.id to billLineToMap(it, tenantId) }, createOnly = false, tenantId)
            uploadCollection("khata_customers", customers.map { it.id to customerToMap(it, tenantId) }, createOnly = false, tenantId)
            uploadCollection("expense_categories", expenseCategories.map { it.id to categoryToMap(it, tenantId) }, createOnly = false, tenantId)
            uploadCollection("expenses", expenses.map { it.id to expenseToMap(it, tenantId) }, createOnly = false, tenantId)
            uploadCollection("owner_drawings", ownerDrawings.map { it.id to drawingToMap(it, tenantId) }, createOnly = false, tenantId)

            // Append-only collections: create-only (never overwrite an existing doc).
            uploadCollection("stock_ledger", stockEntries.map { it.id to stockToMap(it, tenantId) }, createOnly = true, tenantId)
            uploadCollection("khata_entries", khataEntries.map { it.id to khataEntryToMap(it, tenantId) }, createOnly = true, tenantId)
            uploadCollection("cashbook_entries", cashbookEntries.map { it.id to cashbookToMap(it, tenantId) }, createOnly = true, tenantId)

            val now = System.currentTimeMillis()
            cloudSyncDao.updateLastBackupAt(now)
            emit(BackupProgress.Success("ক্লাউড ব্যাকআপ সফলভাবে সম্পন্ন হয়েছে!", now))
        } catch (e: Exception) {
            emit(BackupProgress.Error("ব্যাকআপে ত্রুটি: ${e.localizedMessage ?: "ইন্টারনেট সংযোগ চেক করুন"}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Writes a list of (documentId -> data) into one top-level collection, committing in
     * per-collection batches of at most [BATCH_LIMIT] operations so a single denied write
     * cannot roll back unrelated collections.
     *
     * When [createOnly] is true (append-only collections), existing documents are skipped
     * rather than overwritten, since the live rules deny client updates on those collections.
     */
    private suspend fun uploadCollection(
        collection: String,
        docs: List<Pair<String, Map<String, Any>>>,
        createOnly: Boolean,
        tenantId: String
    ) {
        if (docs.isEmpty()) return

        val existing = if (createOnly) existingDocIds(collection, tenantId) else emptySet()
        val toWrite = docs.filter { it.first !in existing }
        if (toWrite.isEmpty()) return

        var batch = firestore.batch()
        var count = 0
        for ((id, data) in toWrite) {
            batch.set(firestore.collection(collection).document(id), data)
            count++
            if (count >= BATCH_LIMIT) {
                batch.commit().await()
                batch = firestore.batch()
                count = 0
            }
        }
        if (count > 0) {
            batch.commit().await()
        }
    }

    /** Document IDs already present in a top-level collection for this tenant. */
    private suspend fun existingDocIds(collection: String, tenantId: String): Set<String> {
        return try {
            firestore.collection(collection)
                .whereEqualTo("tenantId", tenantId)
                .select(FieldPath.documentId())
                .get()
                .await()
                .documents
                .map { it.id }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // --- Field mapping: enums as String, dates as epoch-millis Long, tenantId = claims ---

    private fun bookToMap(b: Book, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "isbn" to (b.isbn ?: ""),
        "titleBn" to b.titleBn,
        "titleEn" to (b.titleEn ?: ""),
        "author" to b.author,
        "publisher" to b.publisher,
        "classLevel" to b.classLevel,
        "subject" to b.subject,
        "editionYear" to b.editionYear,
        "category" to b.category.name,
        "purchasePrice" to b.purchasePrice,
        "sellingPrice" to b.sellingPrice,
        "initialStock" to b.initialStock,
        "lowStockThreshold" to b.lowStockThreshold,
        "isActive" to b.isActive,
        "createdAt" to b.createdAt,
        "updatedAt" to b.updatedAt
    )

    private fun stockToMap(s: StockLedgerEntry, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "bookId" to s.bookId,
        "changeQuantity" to s.changeQuantity,
        "reason" to s.reason.name,
        "referenceId" to (s.referenceId ?: ""),
        "userId" to s.userId,
        "timestamp" to s.timestamp,
        "idempotencyKey" to s.idempotencyKey
    )

    private fun billToMap(b: Bill, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "billNumber" to b.billNumber,
        "customerId" to (b.customerId ?: ""),
        "customerNameBn" to b.customerNameBn,
        "customerPhone" to b.customerPhone,
        "userId" to b.userId,
        "subtotal" to b.subtotal,
        "discountAmount" to b.discountAmount,
        "discountType" to b.discountType.name,
        "vatAmount" to b.vatAmount,
        "totalAmount" to b.totalAmount,
        "paymentMethod" to b.paymentMethod.name,
        "paidAmount" to b.paidAmount,
        "dueAmount" to b.dueAmount,
        "khataEntryId" to (b.khataEntryId ?: ""),
        "billDate" to b.billDate,
        "syncStatus" to b.syncStatus.name,
        "idempotencyKey" to b.idempotencyKey
    )

    private fun billLineToMap(l: BillLine, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "billId" to l.billId,
        "bookId" to l.bookId,
        "bookTitleBn" to l.bookTitleBn,
        "quantity" to l.quantity,
        "unitPrice" to l.unitPrice,
        "lineTotal" to l.lineTotal,
        "vatAmount" to l.vatAmount
    )

    private fun customerToMap(c: KhataCustomer, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "nameBn" to c.nameBn,
        "phone" to c.phone,
        "address" to c.address,
        "creditLimit" to c.creditLimit,
        "isActive" to c.isActive,
        "createdAt" to c.createdAt,
        "updatedAt" to c.updatedAt
    )

    private fun khataEntryToMap(e: KhataEntry, tenantId: String): Map<String, Any> {
        // amount > 0 requirement: a negative ADJUSTMENT is stored as magnitude with a
        // "Negative Adj: " description prefix.
        var amount = e.amount
        var description = e.description
        if (amount < 0) {
            amount = -amount
            description = "Negative Adj: $description"
        }
        return mapOf(
            "tenantId" to tenantId,
            "customerId" to e.customerId,
            "amount" to amount,
            "type" to e.type.name,
            "description" to description,
            "referenceBillId" to (e.referenceBillId ?: ""),
            "collectedByUserId" to e.collectedByUserId,
            "date" to e.date,
            "idempotencyKey" to e.idempotencyKey
        )
    }

    private fun categoryToMap(c: ExpenseCategory, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "nameBn" to c.nameBn,
        "icon" to c.icon,
        "isActive" to c.isActive
    )

    private fun expenseToMap(e: Expense, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "categoryId" to e.categoryId,
        "amount" to e.amount,
        "description" to e.description,
        "expenseDate" to e.expenseDate,
        "receiptPhotoPath" to (e.receiptPhotoPath ?: ""),
        "userId" to e.userId,
        "idempotencyKey" to e.idempotencyKey
    )

    private fun cashbookToMap(c: CashbookEntry, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "account" to c.account.name,
        "type" to c.type.name,
        "amount" to c.amount,
        "description" to c.description,
        "referenceId" to (c.referenceId ?: ""),
        "date" to c.date,
        "userId" to c.userId,
        "idempotencyKey" to c.idempotencyKey
    )

    private fun drawingToMap(d: OwnerDrawing, tenantId: String) = mapOf(
        "tenantId" to tenantId,
        "amount" to d.amount,
        "description" to d.description,
        "drawingDate" to d.drawingDate,
        "userId" to d.userId,
        "idempotencyKey" to d.idempotencyKey
    )
}
