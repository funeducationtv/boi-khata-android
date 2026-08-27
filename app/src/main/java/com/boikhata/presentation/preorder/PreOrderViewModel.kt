package com.boikhata.presentation.preorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.PreOrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreOrderViewModel @Inject constructor(
    // private val preOrderRepository: PreOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreOrderUiState())
    val uiState: StateFlow<PreOrderUiState> = _uiState

    fun createPreOrder(customerId: String, bookId: String, qty: Int, advance: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // TODO: Save to DB
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "প্রি-অর্ডার সফল হয়েছে!"
            )
        }
    }

    fun updateStatus(orderId: String, status: PreOrderStatus) {
        viewModelScope.launch {
            // TODO: Update status
        }
    }
}

data class PreOrderUiState(
    val isLoading: Boolean = false,
    val orders: List<com.boikhata.domain.model.PreOrderWithDetails> = emptyList(),
    val message: String? = null
)
