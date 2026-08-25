package com.boikhata.data.repository

import com.boikhata.data.local.AuditLogDao
import com.boikhata.domain.model.AuditAction
import com.boikhata.domain.model.LocalAuditLog
import com.boikhata.domain.repository.AuditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class AuditRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao
) : AuditRepository {
    override suspend fun logAction(userId: String, action: AuditAction, detail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            auditLogDao.insert(
                LocalAuditLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    action = action,
                    detail = detail
                )
            )
        }
    }
}
