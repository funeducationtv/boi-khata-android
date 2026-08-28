package com.boikhata.data.repository

import com.boikhata.data.local.CatalogDao
import com.boikhata.domain.model.Book
import com.boikhata.domain.model.BookWithStock
import com.boikhata.domain.model.StockChangeReason
import com.boikhata.domain.model.StockLedgerEntry
import com.boikhata.domain.repository.CatalogRepository
import com.boikhata.security.LicenseWriteGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val catalogDao: CatalogDao,
    private val tenantIdProvider: TenantIdProvider,
    private val writeGuard: LicenseWriteGuard
) : CatalogRepository {

    override fun getBooksWithStock(): Flow<List<BookWithStock>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { catalogDao.getBooksWithStock(it) }

    override suspend fun addBook(book: Book, initialStock: Int, userId: String): Result<Unit> {
        return try {
            writeGuard.assertWritable()
            val tid = tenantIdProvider.current()
            catalogDao.insertBook(book.copy(tenantId = tid, initialStock = initialStock))
            // No initial ledger entry needed as it's tracked in initialStock per blueprint to avoid double-counting
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun adjustStock(bookId: String, changeQty: Int, reason: StockChangeReason, userId: String): Result<Unit> {
        return try {
            writeGuard.assertWritable()
            val tid = tenantIdProvider.current()
            val entry = StockLedgerEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tid,
                bookId = bookId,
                changeQuantity = changeQty,
                reason = reason,
                referenceId = null,
                userId = userId,
                timestamp = System.currentTimeMillis(),
                idempotencyKey = UUID.randomUUID().toString()
            )
            catalogDao.insertStockLedgerEntry(entry)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
