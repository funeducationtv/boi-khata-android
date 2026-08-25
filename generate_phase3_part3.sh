#!/bin/bash
set -e
PKG_DIR="app/src/main/java/com/boikhata"

# 7. UPDATE DATABASE MODULE
cat << 'INNER_EOF' > $PKG_DIR/di/DatabaseModule.kt
package com.boikhata.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boikhata.data.local.*
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
    
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `isbn` TEXT, `titleBn` TEXT NOT NULL, `titleEn` TEXT, `author` TEXT NOT NULL, `publisher` TEXT NOT NULL, `classLevel` TEXT NOT NULL, `subject` TEXT NOT NULL, `editionYear` TEXT NOT NULL, `category` TEXT NOT NULL, `purchasePrice` REAL NOT NULL, `sellingPrice` REAL NOT NULL, `initialStock` INTEGER NOT NULL, `lowStockThreshold` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `stock_ledger` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `changeQuantity` INTEGER NOT NULL, `reason` TEXT NOT NULL, `referenceId` TEXT, `userId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `bills` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `billNumber` TEXT NOT NULL, `customerId` TEXT, `customerNameBn` TEXT NOT NULL, `customerPhone` TEXT NOT NULL, `userId` TEXT NOT NULL, `subtotal` REAL NOT NULL, `discountAmount` REAL NOT NULL, `discountType` TEXT NOT NULL, `vatAmount` REAL NOT NULL, `totalAmount` REAL NOT NULL, `paymentMethod` TEXT NOT NULL, `paidAmount` REAL NOT NULL, `dueAmount` REAL NOT NULL, `khataEntryId` TEXT, `billDate` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `bill_lines` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `billId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `bookTitleBn` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` REAL NOT NULL, `lineTotal` REAL NOT NULL, `vatAmount` REAL NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `khata_entries` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `customerId` TEXT, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `date` INTEGER NOT NULL, `referenceId` TEXT, PRIMARY KEY(`id`))")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "boikhata.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides fun provideTenantDao(db: AppDatabase): TenantDao = db.tenantDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()
    @Provides fun provideCatalogDao(db: AppDatabase): CatalogDao = db.catalogDao()
    @Provides fun provideBillingDao(db: AppDatabase): BillingDao = db.billingDao()
}
INNER_EOF

# 8. UPDATE USER REPO (Repair Phase 2 - Add proper Owner verification for PIN Reset)
cat << 'INNER_EOF' > $PKG_DIR/domain/repository/UserRepository.kt
package com.boikhata.domain.repository

import com.boikhata.domain.model.Role
import com.boikhata.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun addUser(name: String, role: Role, pin: String): Result<Unit>
    suspend fun disableUser(userId: String): Result<Unit>
    suspend fun resetPin(userId: String, ownerPin: String, newPin: String): Result<Unit>
}
INNER_EOF

cat << 'INNER_EOF' > $PKG_DIR/data/repository/UserRepositoryImpl.kt
package com.boikhata.data.repository

import com.boikhata.data.local.UserDao
import com.boikhata.domain.model.AuditAction
import com.boikhata.domain.model.Role
import com.boikhata.domain.model.User
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.UserRepository
import com.boikhata.util.CryptoUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val auditRepo: AuditRepository
) : UserRepository {
    override fun getUsers(): Flow<List<User>> = userDao.getAllUsers()

    override suspend fun addUser(name: String, role: Role, pin: String): Result<Unit> {
        if (role == Role.OWNER) return Result.failure(Exception("Cannot add multiple owners"))
        val count = userDao.getNonOwnerCount()
        if (count >= 3) return Result.failure(Exception("Max 3 staff users allowed in Lite tier"))

        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPin(pin, salt)
        val user = User(
            id = UUID.randomUUID().toString(),
            tenantId = "t_1",
            name = name,
            phone = "",
            role = role,
            pinHash = hash,
            salt = salt,
            isActive = true
        )
        userDao.insertUser(user)
        auditRepo.logAction(user.id, AuditAction.USER_CREATED, "Created user: \$name (\$role)")
        return Result.success(Unit)
    }

    override suspend fun disableUser(userId: String): Result<Unit> {
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        if (user.role == Role.OWNER) return Result.failure(Exception("Cannot disable owner"))
        userDao.updateUser(user.copy(isActive = false))
        auditRepo.logAction(user.id, AuditAction.USER_DISABLED, "Disabled user: \${user.name}")
        return Result.success(Unit)
    }

    override suspend fun resetPin(userId: String, ownerPin: String, newPin: String): Result<Unit> {
        val owner = userDao.getOwner() ?: return Result.failure(Exception("Owner not found"))
        val ownerHash = CryptoUtils.hashPin(ownerPin, owner.salt)
        if (ownerHash != owner.pinHash) return Result.failure(Exception("ভুল ওনার পিন"))
        
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPin(newPin, salt)
        userDao.updateUser(user.copy(pinHash = hash, salt = salt))
        auditRepo.logAction(user.id, AuditAction.PIN_RESET, "Reset PIN for user: \${user.name}")
        return Result.success(Unit)
    }
}
INNER_EOF

