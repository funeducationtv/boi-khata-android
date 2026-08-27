package com.boikhata.presentation.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.ReturnNoteRepository
import com.boikhata.util.SmsWhatsAppManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Return Note screen
 */
data class ReturnNoteUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val originalBill: BillWithLines? = null,
    val customer: KhataCustomer? = null,
    val selectedBooks: Map<String, ReturnLineItem> = emptyMap(),
    val returnReason: ReturnReason = ReturnReason.DAMAGE,
    val bookCondition: BookCondition = BookCondition.DAMAGED,
    val notes: String = "",
    val totalRefund: Double = 0.0
)

data class ReturnLineItem(
    val bookId: String,
    val bookTitle: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val reason: ReturnReason,
    val condition: BookCondition
)

/**
 * ViewModel for managing return/exchange operations
 */
@HiltViewModel
class ReturnNoteViewModel @Inject constructor(
    private val returnNoteRepository: ReturnNoteRepository,
    private val smsWhatsAppManager: SmsWhatsAppManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReturnNoteUiState())
    val uiState: StateFlow<ReturnNoteUiState> = _uiState

    fun loadBillForReturn(billId: String, tenantId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bill = returnNoteRepository.getReturnsForBill(tenantId, billId).firstOrNull()?.let {
                    // If we have returns already, we might want to show them
                    null
                }
                
                // In production, fetch the full bill with lines from BillingRepository
                // For now, we'll set a placeholder
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    originalBill = null // TODO: Fetch from repository
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "বিল লোড করা যায়নি: ${e.message}"
                )
            }
        }
    }

    fun addBookToReturn(
        bookId: String,
        bookTitle: String,
        quantity: Int,
        unitPrice: Double
    ) {
        val currentItems = _uiState.value.selectedBooks.toMutableMap()
        val lineTotal = quantity * unitPrice
        
        currentItems[bookId] = ReturnLineItem(
            bookId = bookId,
            bookTitle = bookTitle,
            quantity = quantity,
            unitPrice = unitPrice,
            lineTotal = lineTotal,
            reason = _uiState.value.returnReason,
            condition = _uiState.value.bookCondition
        )
        
        val totalRefund = currentItems.values.sumOf { it.lineTotal }
        
        _uiState.value = _uiState.value.copy(
            selectedBooks = currentItems,
            totalRefund = totalRefund
        )
    }

    fun removeBookFromReturn(bookId: String) {
        val currentItems = _uiState.value.selectedBooks.toMutableMap()
        currentItems.remove(bookId)
        
        val totalRefund = currentItems.values.sumOf { it.lineTotal }
        
        _uiState.value = _uiState.value.copy(
            selectedBooks = currentItems,
            totalRefund = totalRefund
        )
    }

    fun updateReturnReason(reason: ReturnReason) {
        val updatedItems = _uiState.value.selectedBooks.mapValues { (_, item) ->
            item.copy(reason = reason)
        }
        _uiState.value = _uiState.value.copy(returnReason = reason, selectedBooks = updatedItems)
    }

    fun updateBookCondition(condition: BookCondition) {
        val updatedItems = _uiState.value.selectedBooks.mapValues { (_, item) ->
            item.copy(condition = condition)
        }
        _uiState.value = _uiState.value.copy(bookCondition = condition, selectedBooks = updatedItems)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun submitReturnNote(
        tenantId: String,
        originalBillId: String,
        customerId: String?,
        userId: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val noteId = "return_${System.currentTimeMillis()}"
                val idempotencyKey = "${noteId}_${userId}"
                
                val note = ReturnNote(
                    id = noteId,
                    tenantId = tenantId,
                    originalBillId = originalBillId,
                    customerId = customerId,
                    userId = userId,
                    returnDate = System.currentTimeMillis(),
                    reason = _uiState.value.returnReason,
                    condition = _uiState.value.bookCondition,
                    totalRefund = _uiState.value.totalRefund,
                    status = ReturnStatus.PENDING,
                    notes = _uiState.value.notes,
                    idempotencyKey = idempotencyKey
                )
                
                val lines = _uiState.value.selectedBooks.values.map { item ->
                    ReturnNoteLine(
                        id = "line_${noteId}_${item.bookId}",
                        returnNoteId = noteId,
                        bookId = item.bookId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        lineTotal = item.lineTotal,
                        reason = item.reason,
                        condition = item.condition
                    )
                }
                
                returnNoteRepository.createReturnNote(note, lines)
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "রিটার্ন নোট সফলভাবে তৈরি হয়েছে",
                    selectedBooks = emptyMap(),
                    totalRefund = 0.0,
                    notes = ""
                )
                
                // Notify customer if contact info available
                if (customerId != null && _uiState.value.customer != null) {
                    // Could send SMS/WhatsApp here
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "রিটার্ন নোট তৈরি ব্যর্থ: ${e.message}"
                )
            }
        }
    }

    fun approveReturn(noteId: String, tenantId: String) {
        viewModelScope.launch {
            try {
                returnNoteRepository.approveReturnNote(noteId, tenantId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "রিটার্ন অনুমোদিত হয়েছে"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "অনুমোদন ব্যর্থ: ${e.message}"
                )
            }
        }
    }

    fun rejectReturn(noteId: String, tenantId: String) {
        viewModelScope.launch {
            try {
                returnNoteRepository.rejectReturnNote(noteId, tenantId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "রিটার্ন প্রত্যাখ্যান করা হয়েছে"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "প্রত্যাখ্যান ব্যর্থ: ${e.message}"
                )
            }
        }
    }

    fun completeReturn(noteId: String, tenantId: String) {
        viewModelScope.launch {
            try {
                returnNoteRepository.completeReturnNote(noteId, tenantId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "রিটার্ন সম্পন্ন হয়েছে"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "সম্পন্ন করা ব্যর্থ: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
