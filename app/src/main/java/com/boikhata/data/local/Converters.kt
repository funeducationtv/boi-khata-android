package com.boikhata.data.local

import androidx.room.TypeConverter
import com.boikhata.domain.model.AuditAction
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.Role

class Converters {
    @TypeConverter fun fromRole(role: Role): String = role.name
    @TypeConverter fun toRole(name: String): Role = Role.valueOf(name)
    @TypeConverter fun fromLicenseState(state: LicenseState): String = state.name
    @TypeConverter fun toLicenseState(name: String): LicenseState = LicenseState.valueOf(name)
    @TypeConverter fun fromAuditAction(action: AuditAction): String = action.name
    @TypeConverter fun toAuditAction(name: String): AuditAction = AuditAction.valueOf(name)
}
