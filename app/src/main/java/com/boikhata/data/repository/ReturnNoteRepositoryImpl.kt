package com.boikhata.data.repository

import com.boikhata.data.local.ReturnNoteDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.ReturnNoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for managing return notes.
 * Handles book returns, exchanges, and stock adjustments.
 */
@Singleton
class ReturnNoteRepositoryImpl @Inject constructor(
    private val returnNoteDao: ReturnNoteDao,
    private val catalogDao: CatalogDao,
    private val billingDao: BillingDao
) : ReturnNoteRepository {

    override suspend fun createReturnNote(
        note: ReturnNote,
        lines: List<ReturnNoteLine>
    ) {
        // Validate that the original bill exists
        val originalBill = billingDao.getBillById(note.tenantId, note.originalBillId)
            ?: throw IllegalArgumentException("Original bill not found")

        // Validate line items total matches note total
        val calculatedTotal = lines.sumOf { it.lineTotal }
        if (Math.abs(calculatedTotal - note.totalRefund) > 0.01) {
            throw IllegalArgumentException("Line totals don't match refund amount")
        }

        // Insert note and lines
        returnNoteDao.insertReturnNote(note)
        returnNoteDao.insertReturnNoteLines(lines)

        // Update stock for returned items
        lines.forEach { line ->
            val stockEntry = StockLedgerEntry(
                id = "stock_${note.id}_${line.bookId}",
                tenantId = note.tenantId,
                bookId = line.bookId,
                changeQuantity = line.quantity, // Positive quantity adds back to stock
                reason = StockChangeReason.RETURN,
                referenceId = note.id,
                userId = note.userId,
                timestamp = System.currentTimeMillis(),
                idempotencyKey = "${note.idempotencyKey}_${line.bookId}"
            )
            catalogDao.insertStockLedgerEntry(stockEntry)
        }

        // If customer exists, update their khata entry for refund
        if (note.customerId != null && note.totalRefund > 0) {
            val khataEntry = KhataEntry(
                id = "khata_return_${note.id}",
                tenantId = note.tenantId,
                customerId = note.customerId,
                amount = note.totalRefund,
                type = KhataEntryType.CREDIT, // Credit to customer account
                description = "Return refund for ${note.reason.name} - Note: ${note.id}",
                referenceBillId = note.originalBillId,
                collectedByUserId = note.userId,
                date = note.returnDate,
                idempotencyKey = "${note.idempotencyKey}_khata"
            )
            billingDao.insertKhataEntry(khataEntry)
        }
    }

    override fun getAllReturnNotes(tenantId: String): Flow<List<ReturnNoteWithLines>> {
        return returnNoteDao.getAllReturnNotes(tenantId)
    }

    override suspend fun getReturnNoteById(
        tenantId: String,
        id: String
    ): ReturnNoteWithLines? {
        val note = returnNoteDao.getReturnNoteById(tenantId, id) ?: return null
        val lines = returnNoteDao.getLinesForReturnNote(id)
        return ReturnNoteWithLines(note, lines)
    }

    override suspend fun approveReturnNote(noteId: String, tenantId: String) {
        returnNoteDao.updateReturnNoteStatus(
            noteId = noteId,
            status = ReturnStatus.APPROVED,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun rejectReturnNote(noteId: String, tenantId: String) {
        returnNoteDao.updateReturnNoteStatus(
            noteId = noteId,
            status = ReturnStatus.REJECTED,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun completeReturnNote(noteId: String, tenantId: String) {
        returnNoteDao.updateReturnNoteStatus(
            noteId = noteId,
            status = ReturnStatus.COMPLETED,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun getReturnsForBill(
        tenantId: String,
        billId: String
    ): List<ReturnNote> {
        return returnNoteDao.getReturnsForBill(tenantId, billId)
    }

    override suspend fun getAllReturnNotesDirect(tenantId: String): List<ReturnNote> {
        return returnNoteDao.getAllReturnNotesDirect(tenantId)
    }
}
