package com.boikhata.presentation.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AccountingRepository
import com.boikhata.domain.repository.ProfitAndLossData
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountingViewModel @Inject constructor(
    private val accountingRepo: AccountingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<ExpenseWithCategory>>(emptyList())
    val expenses: StateFlow<List<ExpenseWithCategory>> = _expenses.asStateFlow()

    private val _categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    val categories: StateFlow<List<ExpenseCategory>> = _categories.asStateFlow()

    private val _selectedAccount = MutableStateFlow(CashbookAccount.CASH)
    val selectedAccount: StateFlow<CashbookAccount> = _selectedAccount.asStateFlow()

    private val _cashbookEntries = MutableStateFlow<List<CashbookEntry>>(emptyList())
    val cashbookEntries: StateFlow<List<CashbookEntry>> = _cashbookEntries.asStateFlow()

    private val _pnl = MutableStateFlow(ProfitAndLossData(0.0, 0.0, 0.0, 0.0))
    val pnl: StateFlow<ProfitAndLossData> = _pnl.asStateFlow()

    init {
        viewModelScope.launch {
            accountingRepo.getExpenses().collect { _expenses.value = it }
        }
        viewModelScope.launch {
            accountingRepo.getExpenseCategories().collect { list ->
                if (list.isEmpty()) {
                    // Auto-seed default expense categories
                    listOf("দোকান ভাড়া", "বিদ্যুৎ বিল", "কর্মচারীর বেতন", "চা-নাস্তা ও যাতায়াত", "অন্যান্য").forEach {
                        accountingRepo.createExpenseCategory(it, "ic_tag")
                    }
                } else {
                    _categories.value = list
                }
            }
        }
        viewModelScope.launch {
            accountingRepo.getMonthlyProfitAndLoss(0, Long.MAX_VALUE).collect { _pnl.value = it }
        }
        observeCashbook(CashbookAccount.CASH)
    }

    fun selectAccount(account: CashbookAccount) {
        _selectedAccount.value = account
        observeCashbook(account)
    }

    private fun observeCashbook(account: CashbookAccount) {
        viewModelScope.launch {
            accountingRepo.getCashbookEntries(account).collect {
                _cashbookEntries.value = it
            }
        }
    }

    fun addExpense(categoryId: String, amount: Double, desc: String) {
        val userId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            accountingRepo.addExpense(categoryId, amount, desc, null, userId)
        }
    }

    fun addOpeningBalance(account: CashbookAccount, amount: Double) {
        val userId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            val cash = if (account == CashbookAccount.CASH) amount else 0.0
            val bkash = if (account == CashbookAccount.BKASH) amount else 0.0
            val bank = if (account == CashbookAccount.BANK) amount else 0.0
            accountingRepo.setOpeningBalance(cash, bkash, bank, userId)
        }
    }

    fun addOwnerDrawing(amount: Double, desc: String) {
        val userId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            accountingRepo.addOwnerDrawing(amount, desc, userId)
        }
    }
}
