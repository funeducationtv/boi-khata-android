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
    
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `khata_entries`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `khata_customers` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `nameBn` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `creditLimit` REAL NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `khata_entries` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `description` TEXT NOT NULL, `referenceBillId` TEXT, `collectedByUserId` TEXT NOT NULL, `date` INTEGER NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `expense_categories` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `nameBn` TEXT NOT NULL, `icon` TEXT NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `expenseDate` INTEGER NOT NULL, `receiptPhotoPath` TEXT, `userId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `cashbook_entries` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `account` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `referenceId` TEXT, `date` INTEGER NOT NULL, `userId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `owner_drawings` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `drawingDate` INTEGER NOT NULL, `userId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `master_catalog_books` (`id` TEXT NOT NULL, `isbn` TEXT, `titleBn` TEXT NOT NULL, `titleEn` TEXT, `author` TEXT NOT NULL, `publisher` TEXT NOT NULL, `classLevel` TEXT NOT NULL, `subject` TEXT NOT NULL, `editionYear` TEXT NOT NULL, `mrp` REAL NOT NULL, `isActive` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`id`))")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()
    }

    @Provides fun provideTenantDao(db: AppDatabase): TenantDao = db.tenantDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()
    @Provides fun provideCatalogDao(db: AppDatabase): CatalogDao = db.catalogDao()
    @Provides fun provideBillingDao(db: AppDatabase): BillingDao = db.billingDao()
    @Provides fun provideCloudSyncDao(db: AppDatabase): CloudSyncDao = db.cloudSyncDao()
}

@Module
@InstallIn(SingletonComponent::class)
object Phase4DatabaseModule {
    @Provides fun provideKhataDao(db: AppDatabase): KhataDao = db.khataDao()
    @Provides fun provideAccountingDao(db: AppDatabase): AccountingDao = db.accountingDao()
    @Provides fun provideMasterCatalogDao(db: AppDatabase): MasterCatalogDao = db.masterCatalogDao()
}
