#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

cat << 'INNER_EOF' > $PKG_DIR/domain/repository/DashboardRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.LicenseState
import kotlinx.coroutines.flow.Flow

data class DashboardData(
    val tenantName: String,
    val licenseState: LicenseState,
    val licenseDaysRemaining: Int,
    val todaySales: Int,
    val todayCollection: Int,
    val todayProfit: Int,
    val lowStockCount: Int,
    val collectTodayList: List<CollectItem>
)

data class CollectItem(
    val id: String,
    val name: String,
    val amount: Int,
    val daysOverdue: Int,
    val isCritical: Boolean
)

interface DashboardRepository {
    fun getDashboardData(): Flow<DashboardData>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/MockDashboardRepository.kt
package com.boikhata.data.repository

import com.boikhata.data.local.TenantDao
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.repository.CollectItem
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MockDashboardRepository @Inject constructor(
    private val tenantDao: TenantDao
) : DashboardRepository {
    override fun getDashboardData(): Flow<DashboardData> {
        return tenantDao.getTenant().map { tenant ->
            DashboardData(
                tenantName = tenant?.name ?: "বই ঘর লাইব্রেরি ▾",
                licenseState = tenant?.licenseState ?: LicenseState.GRACE,
                licenseDaysRemaining = tenant?.licenseDaysRemaining ?: 8,
                todaySales = 4500,
                todayCollection = 3200,
                todayProfit = 1150,
                lowStockCount = 3,
                collectTodayList = listOf(
                    CollectItem("1", "রহিম ভাই", 2400, 30, true),
                    CollectItem("2", "করিম স্টোর", 1200, 15, false)
                )
            )
        }
    }
}
INNER_EOF

mkdir -p $PKG_DIR/di
cat << 'INNER_EOF' > $PKG_DIR/di/RepositoryModule.kt
package com.boikhata.di

import com.boikhata.data.repository.MockDashboardRepository
import com.boikhata.domain.repository.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDashboardRepository(
        mockDashboardRepository: MockDashboardRepository
    ): DashboardRepository
}
INNER_EOF

mkdir -p $PKG_DIR/presentation/dashboard
cat << 'INNER_EOF' > $PKG_DIR/presentation/dashboard/DashboardViewModel.kt
package com.boikhata.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.domain.repository.DashboardData
import com.boikhata.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val data: DashboardData) : DashboardUiState
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

    private fun kotlinx.coroutines.flow.Flow<DashboardData>.mapToUiState(): kotlinx.coroutines.flow.Flow<DashboardUiState> {
        return kotlinx.coroutines.flow.map { DashboardUiState.Success(it) }
    }
}
INNER_EOF

chmod +x generate_repo.sh
./generate_repo.sh
