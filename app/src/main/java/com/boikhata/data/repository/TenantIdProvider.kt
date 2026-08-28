package com.boikhata.data.repository

import com.boikhata.data.local.CloudSyncDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the single effective tenant id for local (Room) queries.
 *
 * Before any cloud login the data is tagged "t_1" (the Phase-1/2/3 placeholder). After
 * the first successful cloud login, the one-time rebind (A4) moves local rows to the
 * claims tenantId, and this provider returns that value so local reads stay consistent.
 * Full multi-tenant isolation across many tenants in one DB remains Phase 7.
 */
@Singleton
class TenantIdProvider @Inject constructor(
    private val cloudSyncDao: CloudSyncDao
) {
    suspend fun current(): String =
        cloudSyncDao.getSyncStateDirect()?.tenantId?.takeIf { it.isNotBlank() } ?: DEFAULT_TENANT

    /** Reactive tenant id, so flows re-query after the one-time rebind changes the value. */
    fun tenantIdFlow(): Flow<String> =
        cloudSyncDao.getCloudSyncState()
            .map { it?.tenantId?.takeIf { t -> t.isNotBlank() } ?: DEFAULT_TENANT }

    companion object {
        const val DEFAULT_TENANT = "t_1"
    }
}
