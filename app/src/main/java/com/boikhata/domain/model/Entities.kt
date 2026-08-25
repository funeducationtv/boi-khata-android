package com.boikhata.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey val id: String,
    val name: String,
    val licenseState: LicenseState,
    val licenseDaysRemaining: Int
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val phone: String,
    val role: Role,
    val pinHash: String,
    val salt: String,
    val isActive: Boolean
)

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey val id: String,
    val tenantId: String,
    val hardwareSignature: String,
    val name: String
)

@Entity(tableName = "audit_logs")
data class LocalAuditLog(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val userId: String,
    val action: AuditAction,
    val detail: String
)
