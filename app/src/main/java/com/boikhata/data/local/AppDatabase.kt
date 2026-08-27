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
        CloudSyncState::class, ReturnNote::class, ReturnNoteLine::class
    ],
    version = 6,
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
    abstract fun returnNoteDao(): ReturnNoteDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS return_notes (
                        id TEXT NOT NULL PRIMARY KEY,
                        tenantId TEXT NOT NULL,
                        originalBillId TEXT NOT NULL,
                        customerId TEXT,
                        userId TEXT NOT NULL,
                        returnDate INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        condition TEXT NOT NULL,
                        totalRefund REAL NOT NULL,
                        status TEXT NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        idempotencyKey TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS return_note_lines (
                        id TEXT NOT NULL PRIMARY KEY,
                        returnNoteId TEXT NOT NULL,
                        bookId TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice REAL NOT NULL,
                        lineTotal REAL NOT NULL,
                        reason TEXT NOT NULL,
                        condition TEXT NOT NULL,
                        FOREIGN KEY(returnNoteId) REFERENCES return_notes(id) ON DELETE CASCADE,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_notes_tenantId ON return_notes(tenantId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_notes_originalBillId ON return_notes(originalBillId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_notes_customerId ON return_notes(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_notes_userId ON return_notes(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_note_lines_returnNoteId ON return_note_lines(returnNoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_note_lines_bookId ON return_note_lines(bookId)")
            }
        }

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
