package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a return note for books returned by customers.
 * Essential for managing exchanges, refunds, and stock adjustments in bookstores.
 */
@Entity(
    tableName = "return_notes",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["originalBillId"]),
        Index(value = ["customerId"]),
        Index(value = ["userId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = KhataCustomer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ReturnNote(
    @PrimaryKey val id: String,
    val tenantId: String,
    val originalBillId: String,
    val customerId: String?, // Nullable as returns can happen without customer record
    val userId: String,
    val returnDate: Long,
    val reason: ReturnReason,
    val condition: BookCondition,
    val totalRefund: Double,
    val status: ReturnStatus,
    val notes: String = "",
    val idempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ReturnNoteWithLines(
    val returnNote: ReturnNote,
    val lines: List<ReturnNoteLine>
)

/**
 * Reasons for returning books.
 */
enum class ReturnReason {
    DAMAGE,          // Damaged during delivery or printing defect
    WRONG_BOOK,      // Wrong book delivered
    QUALITY_ISSUE,   // Poor print quality, missing pages
    DUPLICATE,       // Duplicate entry in bill
    CUSTOMER_REQUEST,// Customer changed mind (if within policy)
    OTHER            // Other reasons
}

/**
 * Condition of the book when returned.
 */
enum class BookCondition {
    SEALED,     // Still sealed, untouched
    USED,       // Used but in good condition
    OPENED,     // Opened but not used
    DAMAGED     // Damaged by customer
}

/**
 * Status of the return request.
 */
enum class ReturnStatus {
    PENDING,    // Awaiting approval
    APPROVED,   // Approved for refund/exchange
    REJECTED,   // Rejected
    COMPLETED   // Refund processed or exchange done
}

/**
 * Line items for a return note.
 */
@Entity(
    tableName = "return_note_lines",
    indices = [
        Index(value = ["returnNoteId"]),
        Index(value = ["bookId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ReturnNote::class,
            parentColumns = ["id"],
            childColumns = ["returnNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReturnNoteLine(
    @PrimaryKey val id: String,
    val returnNoteId: String,
    val bookId: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val reason: ReturnReason,
    val condition: BookCondition
)
