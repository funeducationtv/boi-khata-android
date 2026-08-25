package com.boikhata.domain.repository

import com.boikhata.domain.model.MasterCatalogBook
import kotlinx.coroutines.flow.Flow

interface MasterCatalogRepository {
    fun getAllMasterBooks(): Flow<List<MasterCatalogBook>>
    suspend fun seedInitialData(): Result<Unit>
    suspend fun importBookToCatalog(masterBookId: String, purchasePrice: Double, initialStock: Int, userId: String): Result<Unit>
}
