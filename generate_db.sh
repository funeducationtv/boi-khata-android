#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

cat << 'INNER_EOF' > $PKG_DIR/data/local/Converters.kt
package com.boikhata.data.local

import androidx.room.TypeConverter
import com.boikhata.domain.model.LicenseState
import com.boikhata.domain.model.Role

class Converters {
    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(name: String): Role = Role.valueOf(name)

    @TypeConverter
    fun fromLicenseState(state: LicenseState): String = state.name

    @TypeConverter
    fun toLicenseState(name: String): LicenseState = LicenseState.valueOf(name)
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/local/Daos.kt
package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants LIMIT 1")
    fun getTenant(): Flow<Tenant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE tenantId = :tenantId")
    fun getUsersByTenant(tenantId: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/local/AppDatabase.kt
package com.boikhata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boikhata.domain.model.Device
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User

@Database(
    entities = [Tenant::class, User::class, Device::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun userDao(): UserDao
}
INNER_EOF

chmod +x generate_db.sh
./generate_db.sh
