package com.boikhata.presentation.bundles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.model.BundleWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BundlesViewModel @Inject constructor(
    // private val bundleRepository: BundleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BundlesUiState())
    val uiState: StateFlow<BundlesUiState> = _uiState

    fun createBundle(nameBn: String, nameEn: String, price: Double, items: Map<String, Int>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // TODO: Call repository to save bundle
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = "বান্ডল '$nameBn' সফলভাবে তৈরি হয়েছে!"
            )
        }
    }

    fun loadBundles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // TODO: Fetch from DB
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

data class BundlesUiState(
    val isLoading: Boolean = false,
    val bundles: List<BundleWithItems> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)
