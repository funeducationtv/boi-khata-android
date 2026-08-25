package com.boikhata.domain.repository

import com.boikhata.domain.model.AuditAction

interface AuditRepository {
    suspend fun logAction(userId: String, action: AuditAction, detail: String)
}
