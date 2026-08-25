package com.boikhata.data.repository

import com.boikhata.data.local.KhataDao
import com.boikhata.data.local.UserDao
import com.boikhata.domain.model.*
import com.boikhata.domain.repository.AuditRepository
import com.boikhata.domain.repository.KhataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class KhataRepositoryImpl @Inject constructor(
    private val khataDao: KhataDao,
    private val userDao: UserDao,
    private val auditRepo: AuditRepository
) : KhataRepository {
    private val tenantId = "t_1"

    override fun getCustomersWithBalance(): Flow<List<KhataCustomerWithBalance>> =
        khataDao.getCustomersWithBalance(tenantId).map { customers ->
            customers.map { item ->
                // Calculate days since customer created / last active overdue
                val days = if (item.balance > 0) {
                    val diff = System.currentTimeMillis() - item.customer.createdAt
                    (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                } else {
                    0
                }
                item.copy(daysOverdue = days)
            }
        }

    override fun getEntriesForCustomer(customerId: String): Flow<List<KhataEntry>> =
        khataDao.getEntriesForCustomer(tenantId, customerId)

    override suspend fun createCustomer(
        nameBn: String,
        phone: String,
        address: String,
        creditLimit: Double,
        userId: String
    ): Result<KhataCustomer> {
        val user = userDao.getUserById(userId)
        if (user?.role != Role.OWNER) {
            return Result.failure(SecurityException("কেবলমাত্র মালিক (Owner) নতুন খাতা কাস্টমার তৈরি করতে পারেন"))
        }

        return try {
            val customer = KhataCustomer(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
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
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
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
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
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
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
