package com.boikhata.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.LicenseInfo
import com.boikhata.domain.model.SubscriptionPaymentRecord
import com.boikhata.domain.repository.CloudLicenseRepository
import com.boikhata.domain.repository.SubscriptionRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val licenseRepo: CloudLicenseRepository,
    private val subscriptionRepo: SubscriptionRepository,
    val sessionManager: SessionManager
) : ViewModel() {

    private val _licenseInfo = MutableStateFlow<LicenseInfo?>(null)
    val licenseInfo: StateFlow<LicenseInfo?> = _licenseInfo.asStateFlow()

    private val _payments = MutableStateFlow<List<SubscriptionPaymentRecord>>(emptyList())
    val payments: StateFlow<List<SubscriptionPaymentRecord>> = _payments.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        val tenantId = sessionManager.cloudAuthState.value.tenantId
        if (!tenantId.isNullOrBlank()) {
            viewModelScope.launch { licenseRepo.syncLicense(tenantId) }
        }
        viewModelScope.launch {
            licenseRepo.observeLicenseState().collect { _licenseInfo.value = it }
        }
        if (!tenantId.isNullOrBlank()) {
            viewModelScope.launch {
                subscriptionRepo.getPaymentHistory(tenantId).collect { _payments.value = it }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun recordManualPayment(senderPhone: String, trxId: String, note: String) {
        val tenantId = sessionManager.cloudAuthState.value.tenantId
        if (tenantId.isNullOrBlank()) {
            // Pending activation / no cloud tenant: do NOT write (rules would deny).
            _message.value = "ক্লাউড অ্যাকাউন্ট সক্রিয় নয় — ভেন্ডরের সাথে যোগাযোগ করে আগে অ্যাকাউন্ট সক্রিয় করুন।"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = subscriptionRepo.createPaymentRecord(
                tenantId = tenantId,
                amount = 250.0,
                referencePhone = senderPhone,
                trxId = trxId,
                note = note
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                _message.value = "পেমেন্টের তথ্য জমা হয়েছে। যাচাই সাপেক্ষে সাবস্ক্রিপশন সক্রিয় হবে।"
            } else {
                _message.value = "পেমেন্ট তথ্য জমায় ত্রুটি: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
