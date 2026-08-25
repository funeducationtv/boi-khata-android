#!/bin/bash
sed -i '/private val MIGRATION_2_3/i \
    private val MIGRATION_3_4 = object : Migration(3, 4) {\
        override fun migrate(db: SupportSQLiteDatabase) {\
            db.execSQL("DROP TABLE IF EXISTS `khata_entries`")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `khata_customers` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `nameBn` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `creditLimit` REAL NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `khata_entries` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `description` TEXT NOT NULL, `referenceBillId` TEXT, `collectedByUserId` TEXT NOT NULL, `date` INTEGER NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `expense_categories` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `nameBn` TEXT NOT NULL, `icon` TEXT NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `expenseDate` INTEGER NOT NULL, `receiptPhotoPath` TEXT, `userId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `cashbook_entries` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `account` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `referenceId` TEXT, `date` INTEGER NOT NULL, `userId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `owner_drawings` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `drawingDate` INTEGER NOT NULL, `userId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, PRIMARY KEY(`id`))")\
            db.execSQL("CREATE TABLE IF NOT EXISTS `master_catalog_books` (`id` TEXT NOT NULL, `isbn` TEXT, `titleBn` TEXT NOT NULL, `titleEn` TEXT, `author` TEXT NOT NULL, `publisher` TEXT NOT NULL, `classLevel` TEXT NOT NULL, `subject` TEXT NOT NULL, `editionYear` TEXT NOT NULL, `mrp` REAL NOT NULL, `isActive` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`id`))")\
        }\
    }\
' app/src/main/java/com/boikhata/di/DatabaseModule.kt

sed -i 's/.addMigrations(MIGRATION_1_2, MIGRATION_2_3)/.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)/g' app/src/main/java/com/boikhata/di/DatabaseModule.kt

cat << 'INNER_EOF' >> app/src/main/java/com/boikhata/di/DatabaseModule.kt

@Module
@InstallIn(SingletonComponent::class)
object Phase4DatabaseModule {
    @Provides fun provideKhataDao(db: AppDatabase): KhataDao = db.khataDao()
    @Provides fun provideAccountingDao(db: AppDatabase): AccountingDao = db.accountingDao()
    @Provides fun provideMasterCatalogDao(db: AppDatabase): MasterCatalogDao = db.masterCatalogDao()
}
INNER_EOF

