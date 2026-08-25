package com.boikhata.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.Book
import com.boikhata.domain.model.BookCategory
import com.boikhata.domain.repository.CatalogRepository
import com.boikhata.presentation.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val catalogRepo: CatalogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    val books = catalogRepo.getBooksWithStock().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBook(titleBn: String, category: BookCategory, price: Double, initialStock: Int) {
        val userId = sessionManager.currentUser.value?.id ?: return
        val book = Book(
            id = UUID.randomUUID().toString(), tenantId = "", isbn = null,
            titleBn = titleBn, titleEn = null, author = "অজানা", publisher = "অজানা",
            classLevel = "N/A", subject = "N/A", editionYear = "2024", category = category,
            purchasePrice = 0.0, sellingPrice = price, initialStock = initialStock,
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch { catalogRepo.addBook(book, initialStock, userId) }
    }
}
