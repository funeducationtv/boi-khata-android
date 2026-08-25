#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"
mkdir -p $PKG_DIR/{data/local,domain/model,domain/repository,presentation/dashboard,presentation/components,presentation/theme,di}

cat << 'INNER_EOF' > $PKG_DIR/BoiKhataApp.kt
package com.boikhata

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BoiKhataApp : Application()
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/domain/model/Enums.kt
package com.boikhata.domain.model

enum class Role {
    OWNER, MANAGER, SALES, ACCOUNTANT
}

enum class LicenseState {
    ACTIVE, GRACE, SOFT_LOCKED, SUSPENDED
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/domain/model/Entities.kt
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
    val role: Role
)

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey val id: String,
    val tenantId: String,
    val hardwareSignature: String,
    val name: String
)
INNER_EOF

chmod +x generate_code.sh
./generate_code.sh
