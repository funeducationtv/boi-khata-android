package com.boikhata.domain.repository

import com.boikhata.domain.model.ReturnNote
import com.boikhata.domain.model.ReturnNoteLine
import com.boikhata.domain.model.ReturnNoteWithLines
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing return notes.
 * Defines operations for book returns, exchanges, and refunds.
 */
interface ReturnNoteRepository {

    /**
     * Create a new return note with line items.
     * This will:
     * - Validate the original bill
     * - Add stock back to inventory
     * - Create khata entry if customer has credit
     */
    suspend fun createReturnNote(
        note: ReturnNote,
        lines: List<ReturnNoteLine>
    )

    /**
     * Get all return notes for a tenant as a Flow.
     */
    fun getAllReturnNotes(tenantId: String): Flow<List<ReturnNoteWithLines>>

    /**
     * Get a specific return note with its line items.
     */
    suspend fun getReturnNoteById(
        tenantId: String,
        id: String
    ): ReturnNoteWithLines?

    /**
     * Approve a return note (pending -> approved).
     */
    suspend fun approveReturnNote(noteId: String, tenantId: String)

    /**
     * Reject a return note (pending -> rejected).
     */
    suspend fun rejectReturnNote(noteId: String, tenantId: String)

    /**
     * Mark a return note as completed (refund processed or exchange done).
     */
    suspend fun completeReturnNote(noteId: String, tenantId: String)

    /**
     * Get all return notes associated with a specific bill.
     */
    suspend fun getReturnsForBill(
        tenantId: String,
        billId: String
    ): List<ReturnNote>

    /**
     * Get all return notes directly (not as Flow).
     */
    suspend fun getAllReturnNotesDirect(tenantId: String): List<ReturnNote>
}
