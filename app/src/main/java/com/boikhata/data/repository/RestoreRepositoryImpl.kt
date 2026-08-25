package com.boikhata.data.repository

import com.boikhata.data.local.*
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.RestoreProgress
import com.boikhata.domain.repository.RestoreRepository
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
            val snapshot = firestore.collection("bills")
                .whereEqualTo("tenantId", tenantId)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) return@withContext Result.success(true)

            val booksSnap = firestore.collection("books")
                .whereEqualTo("tenantId", tenantId)
                .limit(1)
                .get()
                .await()
            Result.success(!booksSnap.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun performRestore(tenantId: String): Flow<RestoreProgress> = flow {
        if (tenantId.isBlank()) {
            emit(RestoreProgress.Error("দোকানের আইডি (Tenant ID) পাওয়া যায়নি।"))
            return@flow
        }

        try {
            emit(RestoreProgress.Restoring("বইয়ের তালিকা ও স্টক ক্লাউড থেকে নামানো হচ্ছে...", 0.15f))
            val booksSnap = firestore.collection("books").whereEqualTo("tenantId", tenantId).get().await()
            val books = booksSnap.toObjects(Book::class.java)
            if (books.isNotEmpty()) {
                catalogDao.insertBooks(books)
            }

            val stockSnap = firestore.collection("stock_ledger").whereEqualTo("tenantId", tenantId).get().await()
            val stockEntries = stockSnap.toObjects(StockLedgerEntry::class.java)
            if (stockEntries.isNotEmpty()) {
                catalogDao.insertStockLedgerEntries(stockEntries)
            }

            emit(RestoreProgress.Restoring("বিল ও বিক্রির হিসাব নামানো হচ্ছে...", 0.4f))
            val billsSnap = firestore.collection("bills").whereEqualTo("tenantId", tenantId).get().await()
            val bills = billsSnap.toObjects(Bill::class.java)
            if (bills.isNotEmpty()) {
                billingDao.insertBills(bills)
            }

            val billLinesSnap = firestore.collection("bill_lines").whereEqualTo("tenantId", tenantId).get().await()
            val billLines = billLinesSnap.toObjects(BillLine::class.java)
            if (billLines.isNotEmpty()) {
                billingDao.insertBillLines(billLines)
            }

            emit(RestoreProgress.Restoring("খাতার কাস্টমার ও বাকি হিসাব নামানো হচ্ছে...", 0.65f))
            val custSnap = firestore.collection("khata_customers").whereEqualTo("tenantId", tenantId).get().await()
            val customers = custSnap.toObjects(KhataCustomer::class.java)
            if (customers.isNotEmpty()) {
                khataDao.insertCustomers(customers)
            }

            val khataEntriesSnap = firestore.collection("khata_entries").whereEqualTo("tenantId", tenantId).get().await()
            val khataEntries = khataEntriesSnap.toObjects(KhataEntry::class.java)
            if (khataEntries.isNotEmpty()) {
                khataDao.insertEntries(khataEntries)
            }

            emit(RestoreProgress.Restoring("খরচ ও ক্যাশ বুক হিসাব নামানো হচ্ছে...", 0.85f))
            val expSnap = firestore.collection("expenses").whereEqualTo("tenantId", tenantId).get().await()
            val expenses = expSnap.toObjects(Expense::class.java)
            if (expenses.isNotEmpty()) {
                accountingDao.insertExpenses(expenses)
            }

            val cbSnap = firestore.collection("cashbook_entries").whereEqualTo("tenantId", tenantId).get().await()
            val cashbookEntries = cbSnap.toObjects(CashbookEntry::class.java)
            if (cashbookEntries.isNotEmpty()) {
                accountingDao.insertCashbookEntries(cashbookEntries)
            }

            val now = System.currentTimeMillis()
            cloudSyncDao.updateLastRestoreAt(now)

            emit(RestoreProgress.Success("ক্লাউড থেকে সমস্ত ডাটা সফলভাবে রিস্টোর হয়েছে!"))

        } catch (e: Exception) {
            emit(RestoreProgress.Error("ডাটা রিস্টোরে ত্রুটি: ${e.localizedMessage ?: "ইন্টারনেট সংযোগ চেক করুন"}"))
        }
    }.flowOn(Dispatchers.IO)
}
