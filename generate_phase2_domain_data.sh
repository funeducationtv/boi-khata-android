#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# ENUMS
cat << 'INNER_EOF' > $PKG_DIR/domain/model/Enums.kt
package com.boikhata.domain.model

enum class Role {
    OWNER, MANAGER, SALES, ACCOUNTANT
}

enum class LicenseState {
    ACTIVE, GRACE, SOFT_LOCKED, SUSPENDED
}

enum class AuditAction {
    LOGIN_SUCCESS, LOGIN_FAIL, ROLE_SWITCH, USER_CREATED, USER_DISABLED, PIN_RESET
}
INNER_EOF

# ENTITIES
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
INNER_EOF

# DAOS
cat << 'INNER_EOF' > $PKG_DIR/data/local/Daos.kt
package com.boikhata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.domain.model.LocalAuditLog
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
    @Query("SELECT * FROM users WHERE tenantId = :tenantId AND isActive = 1")
    fun getUsersByTenant(tenantId: String): Flow<List<User>>
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE role = 'OWNER' LIMIT 1")
    suspend fun getOwner(): User?
    
    @Query("SELECT COUNT(*) FROM users WHERE role = 'OWNER'")
    fun getOwnerCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role != 'OWNER' AND isActive = 1")
    suspend fun getNonOwnerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(log: LocalAuditLog)
}
INNER_EOF

# CONVERTERS
cat << 'INNER_EOF' > $PKG_DIR/data/local/Converters.kt
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
INNER_EOF

# DATABASE
cat << 'INNER_EOF' > $PKG_DIR/data/local/AppDatabase.kt
package com.boikhata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boikhata.domain.model.Device
import com.boikhata.domain.model.LocalAuditLog
import com.boikhata.domain.model.Tenant
import com.boikhata.domain.model.User

@Database(
    entities = [Tenant::class, User::class, Device::class, LocalAuditLog::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
}
INNER_EOF

# DB MODULE
cat << 'INNER_EOF' > $PKG_DIR/di/DatabaseModule.kt
package com.boikhata.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boikhata.data.local.AppDatabase
import com.boikhata.data.local.AuditLogDao
import com.boikhata.data.local.TenantDao
import com.boikhata.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN pinHash TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN salt TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `userId` TEXT NOT NULL, `action` TEXT NOT NULL, `detail` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "boikhata.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides fun provideTenantDao(db: AppDatabase): TenantDao = db.tenantDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()
}
INNER_EOF

# CRYPTO UTILS
mkdir -p $PKG_DIR/util
cat << 'INNER_EOF' > $PKG_DIR/util/CryptoUtils.kt
package com.boikhata.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CryptoUtils {
    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, saltStr: String): String {
        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
INNER_EOF

# BIOMETRIC STUB
mkdir -p $PKG_DIR/domain/biometric
cat << 'INNER_EOF' > $PKG_DIR/domain/biometric/BiometricAuth.kt
package com.boikhata.domain.biometric

interface BiometricAuth {
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(): Boolean
}

class BiometricAuthStub : BiometricAuth {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun authenticate(): Boolean = false
}
INNER_EOF

# SESSION MANAGER
cat << 'INNER_EOF' > $PKG_DIR/presentation/SessionManager.kt
package com.boikhata.presentation

import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastActivityTime = System.currentTimeMillis()

    fun setSession(user: User) {
        _currentUser.value = user
        _isLocked.value = false
        updateActivity()
    }

    fun lock() {
        if (_currentUser.value != null) {
            _isLocked.value = true
        }
    }

    fun logout() {
        _currentUser.value = null
        _isLocked.value = true
    }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun checkAutoLock() {
        // 5 minutes auto-lock
        if (_currentUser.value != null && !_isLocked.value) {
            if (System.currentTimeMillis() - lastActivityTime > 5 * 60 * 1000) {
                lock()
            }
        }
    }
}
INNER_EOF

