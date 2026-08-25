package com.boikhata.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val data: DashboardData?) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    dashboardRepository: DashboardRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = dashboardRepository.getDashboardData()
        .mapToUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )

    private fun kotlinx.coroutines.flow.Flow<DashboardData?>.mapToUiState(): kotlinx.coroutines.flow.Flow<DashboardUiState> {
        return this.map { DashboardUiState.Success(it) }
    }
}
