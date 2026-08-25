package com.boikhata.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.MasterCatalogBook
import com.boikhata.domain.repository.MasterCatalogRepository
import com.boikhata.domain.repository.RemoteMasterCatalogRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MasterCatalogViewModel @Inject constructor(
    private val masterRepo: MasterCatalogRepository,
    private val remoteCatalogRepo: RemoteMasterCatalogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _books = MutableStateFlow<List<MasterCatalogBook>>(emptyList())
    val books: StateFlow<List<MasterCatalogBook>> = _books.asStateFlow()

    private val _hasRemoteUpdates = MutableStateFlow(false)
    val hasRemoteUpdates: StateFlow<Boolean> = _hasRemoteUpdates.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            masterRepo.seedInitialData()
            masterRepo.getAllMasterBooks().collect { _books.value = it }
        }
        viewModelScope.launch {
            remoteCatalogRepo.checkForRemoteUpdates().collect {
                _hasRemoteUpdates.value = it
            }
        }
    }

    fun clearMessage() {
        _syncMessage.value = null
    }

    fun syncRemotePrices() {
        viewModelScope.launch {
            val result = remoteCatalogRepo.syncMasterCatalogFromCloud()
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _hasRemoteUpdates.value = false
                _syncMessage.value = if (count > 0) "${count}টি বইয়ের নতুন দাম আপডেট হয়েছে!" else "ক্যাটালগ ইতিমধ্যে আপ-টু-ডেট।"
            } else {
                _syncMessage.value = "দাম আপডেটে ত্রুটি: ${result.exceptionOrNull()?.message ?: "ইন্টারনেট চেক করুন"}"
            }
        }
    }

    fun importBook(book: MasterCatalogBook, purchasePrice: Double, initialStock: Int) {
        val userId = sessionManager.currentUser.value?.id ?: "user_1"
        viewModelScope.launch {
            val res = masterRepo.importBookToCatalog(book.id, purchasePrice, initialStock, userId)
            if (res.isSuccess) {
                _syncMessage.value = "${book.titleBn} সফলভাবে আপনার দোকানে যুক্ত হয়েছে।"
            }
        }
    }
}
