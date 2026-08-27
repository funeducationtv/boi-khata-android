package com.boikhata.security

import com.boikhata.data.local.dao.AuditLogDao
import com.boikhata.domain.model.LocalAuditLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val sessionManager: com.boikhata.data.repository.SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    enum class Action {
        LOGIN, LOGOUT, CREATE_BILL, DELETE_BILL, UPDATE_STOCK, 
        EXPORT_REPORT, CHANGE_SETTINGS, SYNC_DATA, VIEW_CUSTOMER_DUE
    }

    fun log(action: Action, detail: String, entityId: String? = null) {
        scope.launch {
            val currentUser = sessionManager.getCurrentUser()
            val log = LocalAuditLog(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                userId = currentUser?.id ?: "unknown",
                userName = currentUser?.nameBn ?: "Unknown",
                action = action.name,
                detail = detail,
                entityId = entityId,
                entityType = null,
                oldValue = null,
                newValue = null,
                ipAddress = null,
                deviceId = sessionManager.getCurrentDeviceId()
            )
            auditLogDao.insert(log)
        }
    }
}
