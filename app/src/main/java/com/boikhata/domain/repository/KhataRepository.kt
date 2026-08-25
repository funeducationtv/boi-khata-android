package com.boikhata.domain.repository

import com.boikhata.domain.model.KhataCustomer
import com.boikhata.domain.model.KhataCustomerWithBalance
import com.boikhata.domain.model.KhataEntry
import com.boikhata.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface KhataRepository {
    fun getCustomersWithBalance(): Flow<List<KhataCustomerWithBalance>>
    fun getEntriesForCustomer(customerId: String): Flow<List<KhataEntry>>
    suspend fun createCustomer(nameBn: String, phone: String, address: String, creditLimit: Double, userId: String): Result<KhataCustomer>
    suspend fun recordPayment(customerId: String, amount: Double, method: PaymentMethod, userId: String): Result<KhataEntry>
    suspend fun addManualCredit(customerId: String, amount: Double, description: String, userId: String): Result<KhataEntry>
}
