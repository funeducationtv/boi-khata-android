#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/boikhata/data/local/AppDatabase.kt
package com.boikhata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boikhata.domain.model.*

@Database(
    entities = [
        Tenant::class, User::class, Device::class, LocalAuditLog::class,
        Book::class, StockLedgerEntry::class, Bill::class, BillLine::class,
        KhataEntry::class, KhataCustomer::class, ExpenseCategory::class,
        Expense::class, CashbookEntry::class, OwnerDrawing::class, MasterCatalogBook::class
    ],
    version = 4,
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
}
INNER_EOF
