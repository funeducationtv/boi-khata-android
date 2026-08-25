package com.boikhata.data.repository

import com.boikhata.data.local.*
import com.boikhata.domain.repository.BackupProgress
import com.boikhata.domain.repository.BackupRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    override fun getLastBackupTime(): Flow<Long?> {
        return cloudSyncDao.getCloudSyncState().map { it?.lastBackupAt }
    }

    override fun performBackup(tenantId: String): Flow<BackupProgress> = flow {
        if (tenantId.isBlank()) {
            emit(BackupProgress.Error("দোকানের আইডি (Tenant ID) পাওয়া যায়নি।"))
            return@flow
        }

        try {
            emit(BackupProgress.BackingUp("বইয়ের স্টক প্রস্তুত হচ্ছে...", 0.1f))
            val books = catalogDao.getAllBooksDirect(tenantId)
            val stockEntries = catalogDao.getAllStockEntriesDirect(tenantId)

            emit(BackupProgress.BackingUp("বিক্রির রশিদ ও বিল প্রস্তুত হচ্ছে...", 0.3f))
            val bills = billingDao.getAllBillsDirect(tenantId)
            val billLines = billingDao.getAllBillLinesDirect(tenantId)

            emit(BackupProgress.BackingUp("খাতার হিসাব ও কাস্টমার প্রস্তুত হচ্ছে...", 0.5f))
            val customers = khataDao.getAllCustomersDirect(tenantId)
            val khataEntries = khataDao.getAllKhataEntriesDirect(tenantId)

            emit(BackupProgress.BackingUp("খরচ ও ক্যাশ বুক প্রস্তুত হচ্ছে...", 0.7f))
            val expenses = accountingDao.getAllExpensesDirect(tenantId)
            val cashbookEntries = accountingDao.getAllCashbookEntriesDirect(tenantId)

            emit(BackupProgress.BackingUp("ক্লাউডে ডাটা আপলোড হচ্ছে...", 0.85f))

            // Batched write to top-level collections
            var currentBatch = firestore.batch()
            var operationCount = 0

            suspend fun commitIfFull() {
                if (operationCount >= 450) {
                    currentBatch.commit().await()
                    currentBatch = firestore.batch()
                    operationCount = 0
                }
            }

            // 1. Books
            for (book in books) {
                val docRef = firestore.collection("books").document(book.id)
                currentBatch.set(docRef, book)
                operationCount++
                commitIfFull()
            }

            // 2. Stock Ledger
            for (stock in stockEntries) {
                val docRef = firestore.collection("stock_ledger").document(stock.id)
                currentBatch.set(docRef, stock)
                operationCount++
                commitIfFull()
            }

            // 3. Bills
            for (bill in bills) {
                val docRef = firestore.collection("bills").document(bill.id)
                currentBatch.set(docRef, bill)
                operationCount++
                commitIfFull()
            }

            // 4. Bill Lines
            for (line in billLines) {
                val docRef = firestore.collection("bill_lines").document(line.id)
                currentBatch.set(docRef, line)
                operationCount++
                commitIfFull()
            }

            // 5. Khata Customers
            for (cust in customers) {
                val docRef = firestore.collection("khata_customers").document(cust.id)
                currentBatch.set(docRef, cust)
                operationCount++
                commitIfFull()
            }

            // 6. Khata Entries
            for (entry in khataEntries) {
                val docRef = firestore.collection("khata_entries").document(entry.id)
                currentBatch.set(docRef, entry)
                operationCount++
                commitIfFull()
            }

            // 7. Expenses
            for (exp in expenses) {
                val docRef = firestore.collection("expenses").document(exp.id)
                currentBatch.set(docRef, exp)
                operationCount++
                commitIfFull()
            }

            // 8. Cashbook Entries
            for (cb in cashbookEntries) {
                val docRef = firestore.collection("cashbook_entries").document(cb.id)
                currentBatch.set(docRef, cb)
                operationCount++
                commitIfFull()
            }

            if (operationCount > 0) {
                currentBatch.commit().await()
            }

            val now = System.currentTimeMillis()
            cloudSyncDao.updateLastBackupAt(now)

            emit(BackupProgress.Success("ক্লাউড ব্যাকআপ সফলভাবে সম্পন্ন হয়েছে!", now))

        } catch (e: Exception) {
            emit(BackupProgress.Error("ব্যাকআপে ত্রুটি: ${e.localizedMessage ?: "ইন্টারনেট সংযোগ চেক করুন"}"))
        }
    }.flowOn(Dispatchers.IO)
}
