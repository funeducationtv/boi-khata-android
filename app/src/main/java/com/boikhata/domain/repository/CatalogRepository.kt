package com.boikhata.domain.repository

import com.boikhata.domain.model.Book
import com.boikhata.domain.model.BookWithStock
import com.boikhata.domain.model.StockChangeReason
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun getBooksWithStock(): Flow<List<BookWithStock>>
    suspend fun addBook(book: Book, initialStock: Int, userId: String): Result<Unit>
    suspend fun adjustStock(bookId: String, changeQty: Int, reason: StockChangeReason, userId: String): Result<Unit>
}
