package com.boikhata.presentation.khata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.KhataCustomerWithBalance
import com.boikhata.domain.model.KhataEntry
import com.boikhata.domain.model.PaymentMethod
import com.boikhata.domain.repository.KhataRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KhataViewModel @Inject constructor(
    private val khataRepo: KhataRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _customers = MutableStateFlow<List<KhataCustomerWithBalance>>(emptyList())
    val customers: StateFlow<List<KhataCustomerWithBalance>> = _customers.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        viewModelScope.launch {
            khataRepo.getCustomersWithBalance().collect { _customers.value = it }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun createCustomer(name: String, phone: String, address: String, creditLimit: Double) {
        if (_isSubmitting.value) return
        val currentUserId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = khataRepo.createCustomer(name, phone, address, creditLimit, currentUserId)
            _isSubmitting.value = false
            if (result.isFailure) {
                _message.value = result.exceptionOrNull()?.message ?: "কাস্টমার যোগ করতে ত্রুটি"
            } else {
                _message.value = "কাস্টমার সফলভাবে যুক্ত হয়েছে"
            }
        }
    }

    fun recordPayment(customerId: String, amount: Double, method: PaymentMethod) {
        if (_isSubmitting.value) return
        val currentUserId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = khataRepo.recordPayment(customerId, amount, method, currentUserId)
            _isSubmitting.value = false
            if (result.isSuccess) {
                _message.value = "৳${amount} টাকা আদায় সফলভাবে রেকর্ড করা হয়েছে"
            } else {
                _message.value = "পেমেন্ট রেকর্ডে ত্রুটি: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun addManualCredit(customerId: String, amount: Double, description: String) {
        if (_isSubmitting.value) return
        val currentUserId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = khataRepo.addManualCredit(customerId, amount, description, currentUserId)
            _isSubmitting.value = false
            if (result.isSuccess) {
                _message.value = "বাকি সফলভাবে যুক্ত করা হয়েছে"
            } else {
                _message.value = "বাকি যুক্ত করতে ত্রুটি: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun getCustomerEntries(customerId: String): Flow<List<KhataEntry>> {
        return khataRepo.getEntriesForCustomer(customerId)
    }
}
