package com.boikhata.data.repository

import com.boikhata.data.local.MasterCatalogDao
import com.boikhata.data.local.CatalogDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.MasterCatalogRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class MasterCatalogRepositoryImpl @Inject constructor(
    private val masterDao: MasterCatalogDao,
    private val catalogDao: CatalogDao,
    private val auditRepo: AuditRepository
) : MasterCatalogRepository {
    private val tenantId = "t_1"

    override fun getAllMasterBooks(): Flow<List<MasterCatalogBook>> = masterDao.getAllMasterBooks()

    override suspend fun seedInitialData(): Result<Unit> {
        return try {
            val list = mutableListOf<MasterCatalogBook>()
            val classes = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 9", "Class 10")
            val subjects = listOf("Bangla", "English", "Math", "Science", "Social Science", "Religion", "ICT")
            
            var c = 1
            for (cls in classes) {
                for (sub in subjects) {
                    list.add(MasterCatalogBook(
                        id = UUID.randomUUID().toString(), isbn = "978-984-$c-000", titleBn = "NCTB $sub $cls",
                        titleEn = "$sub $cls", author = "NCTB", publisher = "NCTB", classLevel = cls, subject = sub,
                        editionYear = "2024", mrp = 150.0 + (c * 10), isActive = true, lastUpdated = System.currentTimeMillis()
                    ))
                    c++
                    if(c > 50) break
                }
                if(c > 50) break
            }
            masterDao.insertAll(list)
            Result.success(Unit)
        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importBookToCatalog(masterBookId: String, purchasePrice: Double, initialStock: Int, userId: String): Result<Unit> {
        return try {
            val masterBook = masterDao.getMasterBookById(masterBookId)
                ?: return Result.failure(IllegalArgumentException("মাস্টার বই পাওয়া যায়নি"))

            val now = System.currentTimeMillis()
            val bookId = UUID.randomUUID().toString()
            val book = Book(
                id = bookId,
                tenantId = tenantId,
                isbn = masterBook.isbn,
                titleBn = masterBook.titleBn,
                titleEn = masterBook.titleEn,
                author = masterBook.author,
                publisher = masterBook.publisher,
                classLevel = masterBook.classLevel,
                subject = masterBook.subject,
                editionYear = masterBook.editionYear,
                category = BookCategory.NCTB,
                purchasePrice = purchasePrice,
                sellingPrice = masterBook.mrp,
                initialStock = initialStock,
                lowStockThreshold = 5,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
            catalogDao.insertBook(book)

            if (initialStock > 0) {
                val stockEntry = StockLedgerEntry(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    bookId = bookId,
                    changeQuantity = initialStock,
                    reason = StockChangeReason.PURCHASE,
                    referenceId = null,
                    userId = userId,
                    timestamp = now,
                    idempotencyKey = UUID.randomUUID().toString()
                )
                catalogDao.insertStockLedgerEntry(stockEntry)
            }

            auditRepo.logAction(userId, AuditAction.CATALOG_IMPORTED, "Imported book ${masterBook.titleBn} (Stock: $initialStock, Purchase: $purchasePrice)")
            Result.success(Unit)
        } catch(e: Exception) {
            Result.failure(e)
        }
    }
}
