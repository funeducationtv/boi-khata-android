package com.boikhata.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.BillingRepository
import com.boikhata.domain.repository.CatalogRepository
import com.boikhata.presentation.SessionManager
import com.boikhata.util.VatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepo: BillingRepository,
    private val catalogRepo: CatalogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    val catalog = catalogRepo.getBooksWithStock().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills = billingRepo.getAllBills().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<BillLine>>(emptyList())
    val cart = _cart.asStateFlow()
    
    private val _createdBill = MutableStateFlow<Bill?>(null)
    val createdBill = _createdBill.asStateFlow()

    fun addToCart(bookWithStock: BookWithStock, quantity: Int) {
        val book = bookWithStock.book
        val vat = VatCalculator.calculateVat(book.category, book.sellingPrice, quantity)
        val line = BillLine(
            id = "", tenantId = "", billId = "", bookId = book.id,
            bookTitleBn = book.titleBn, quantity = quantity, unitPrice = book.sellingPrice,
            lineTotal = book.sellingPrice * quantity, vatAmount = vat
        )
        _cart.value = _cart.value + line
    }

    fun checkout(customerName: String, phone: String, paid: Double, method: PaymentMethod) {
        val user = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            billingRepo.createBill(
                userId = user.id, customerName = customerName, customerPhone = phone,
                lines = _cart.value, discountAmount = 0.0, discountType = DiscountType.NONE,
                paymentMethod = method, paidAmount = paid
            ).onSuccess { 
                _createdBill.value = it
                _cart.value = emptyList()
            }
        }
    }
    
    fun voidBill(billId: String) {
        val user = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            billingRepo.voidBill(billId, user.id)
        }
    }
}
