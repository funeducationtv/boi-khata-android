package com.boikhata.data.repository

import com.boikhata.data.local.KhataDao
import com.boikhata.data.local.UserDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.KhataRepository
import com.boikhata.security.LicenseWriteGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID
import javax.inject.Inject

class KhataRepositoryImpl @Inject constructor(
    private val khataDao: KhataDao,
    private val userDao: UserDao,
    private val tenantIdProvider: TenantIdProvider,
    private val writeGuard: LicenseWriteGuard,
    private val auditRepo: AuditRepository
) : KhataRepository {

    override fun getCustomersWithBalance(): Flow<List<KhataCustomerWithBalance>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { tid ->
            combine(
                khataDao.getCustomersWithBalance(tid),
                khataDao.getAllKhataEntries(tid)
            ) { customers, entries ->
                customers.map { item ->
                    val days = fifoDaysOverdue(item.customer.id, item.balance, entries)
                    item.copy(daysOverdue = days)
                }
            }
        }

    override fun getEntriesForCustomer(customerId: String): Flow<List<KhataEntry>> =
        tenantIdProvider.tenantIdFlow().flatMapLatest { khataDao.getEntriesForCustomer(it, customerId) }

    override suspend fun createCustomer(
        nameBn: String,
        phone: String,
        address: String,
        creditLimit: Double,
        userId: String
    ): Result<KhataCustomer> {
        return try {
            writeGuard.assertWritable()
            requireRole(userId, setOf(Role.OWNER), "কেবলমাত্র মালিক (Owner) নতুন খাতা কাস্টমার তৈরি করতে পারেন")
            val customer = KhataCustomer(
                id = UUID.randomUUID().toString(),
                tenantId = tenantIdProvider.current(),
                nameBn = nameBn,
                phone = phone,
                address = address,
                creditLimit = creditLimit,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            khataDao.insertCustomer(customer)
            auditRepo.logAction(userId, AuditAction.KHATA_CUSTOMER_CREATED, "Created khata customer ${customer.nameBn}")
            Result.success(customer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPayment(customerId: String, amount: Double, method: PaymentMethod, userId: String): Result<KhataEntry> {
        return try {
            writeGuard.assertWritable()
            requireRole(userId, setOf(Role.OWNER, Role.MANAGER, Role.ACCOUNTANT), "এই ভূমিকার জন্য পেমেন্ট রেকর্ড করার অনুমতি নেই")
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tenantIdProvider.current(),
                customerId = customerId,
                amount = amount,
                type = KhataEntryType.PAYMENT,
                description = "Collected via $method",
                referenceBillId = null,
                collectedByUserId = userId,
                date = System.currentTimeMillis(),
                idempotencyKey = UUID.randomUUID().toString()
            )
            khataDao.insertEntry(entry)
            auditRepo.logAction(userId, AuditAction.KHATA_PAYMENT_RECORDED, "Recorded payment of $amount for customer $customerId")
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addManualCredit(customerId: String, amount: Double, description: String, userId: String): Result<KhataEntry> {
        return try {
            writeGuard.assertWritable()
            requireRole(userId, setOf(Role.OWNER), "কেবলমাত্র মালিক (Owner) ম্যানুয়াল বাকি যোগ করতে পারেন")
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tenantIdProvider.current(),
                customerId = customerId,
                amount = amount,
                type = KhataEntryType.CREDIT,
                description = description,
                referenceBillId = null,
                collectedByUserId = userId,
                date = System.currentTimeMillis(),
                idempotencyKey = UUID.randomUUID().toString()
            )
            khataDao.insertEntry(entry)
            auditRepo.logAction(userId, AuditAction.KHATA_CREDIT_ADDED, "Added manual credit of $amount for customer $customerId")
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun requireRole(userId: String, allowed: Set<Role>, message: String) {
        val user = userDao.getUserById(userId) ?: throw SecurityException(message)
        if (user.role !in allowed) throw SecurityException(message)
    }

    /**
     * FIFO aging: sort entries by date, apply payments against the OLDEST credits first;
     * the first credit still not fully covered defines the overdue start date.
     */
    private fun fifoDaysOverdue(customerId: String, balance: Double, allEntries: List<KhataEntry>): Int {
        if (balance <= 0) return 0

        val entries = allEntries.filter { it.customerId == customerId }

        val credits = entries.filter { e ->
            e.type == KhataEntryType.CREDIT || e.type == KhataEntryType.OPENING ||
                (e.type == KhataEntryType.ADJUSTMENT && e.amount > 0)
        }.sortedBy { it.date }

        var remainingPayment = entries.sumOf { e ->
            when {
                e.type == KhataEntryType.PAYMENT -> e.amount
                e.type == KhataEntryType.ADJUSTMENT && e.amount < 0 -> -e.amount
                else -> 0.0
            }
        }

        for (credit in credits) {
            if (remainingPayment >= credit.amount) {
                remainingPayment -= credit.amount
            } else {
                val diff = System.currentTimeMillis() - credit.date
                return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            }
        }
        return 0
    }
}
