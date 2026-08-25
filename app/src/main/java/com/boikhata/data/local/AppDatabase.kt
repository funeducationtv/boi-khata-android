package com.boikhata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boikhata.domain.model.*

@Database(
    entities = [
        Tenant::class, User::class, Device::class, LocalAuditLog::class,
        Book::class, StockLedgerEntry::class, Bill::class, BillLine::class,
        KhataEntry::class, KhataCustomer::class, ExpenseCategory::class,
        Expense::class, CashbookEntry::class, OwnerDrawing::class, MasterCatalogBook::class,
        CloudSyncState::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class, Phase3Converters::class, Phase4Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun catalogDao(): CatalogDao
    abstract fun khataDao(): KhataDao
    abstract fun accountingDao(): AccountingDao
    abstract fun masterCatalogDao(): MasterCatalogDao
    abstract fun billingDao(): BillingDao
    abstract fun cloudSyncDao(): CloudSyncDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cloud_sync_state (
                        id TEXT NOT NULL PRIMARY KEY,
                        tenantId TEXT NOT NULL,
                        cloudPhone TEXT,
                        cloudRole TEXT,
                        isPendingActivation INTEGER NOT NULL DEFAULT 0,
                        lastBackupAt INTEGER,
                        lastRestoreAt INTEGER,
                        lastCatalogSyncAt INTEGER,
                        licenseExpiresAt INTEGER,
                        licenseState TEXT NOT NULL DEFAULT 'ACTIVE',
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
