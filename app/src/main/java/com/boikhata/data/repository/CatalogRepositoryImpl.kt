package com.boikhata.data.repository

import com.boikhata.data.local.CatalogDao
import com.boikhata.domain.model.Book
import com.boikhata.domain.model.BookWithStock
import com.boikhata.domain.model.StockChangeReason
import com.boikhata.domain.model.StockLedgerEntry
import com.boikhata.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val catalogDao: CatalogDao
) : CatalogRepository {
    private val tenantId = "t_1" // Hardcoded for single-tenant offline mode in Phase 3

    override fun getBooksWithStock(): Flow<List<BookWithStock>> = catalogDao.getBooksWithStock(tenantId)

    override suspend fun addBook(book: Book, initialStock: Int, userId: String): Result<Unit> {
        try {
            catalogDao.insertBook(book.copy(tenantId = tenantId, initialStock = initialStock))
            // No initial ledger entry needed as it's tracked in initialStock per blueprint to avoid double-counting
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun adjustStock(bookId: String, changeQty: Int, reason: StockChangeReason, userId: String): Result<Unit> {
        try {
            val entry = StockLedgerEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                bookId = bookId,
                changeQuantity = changeQty,
                reason = reason,
                referenceId = null,
                userId = userId,
                timestamp = System.currentTimeMillis(),
                idempotencyKey = UUID.randomUUID().toString()
            )
            catalogDao.insertStockLedgerEntry(entry)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
