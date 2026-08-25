package com.boikhata.data.repository

import com.boikhata.domain.model.SubscriptionPaymentRecord
import com.boikhata.domain.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor() : SubscriptionRepository {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override suspend fun createPaymentRecord(
        tenantId: String,
        amount: Double,
        referencePhone: String,
        notes: String
    ): Result<SubscriptionPaymentRecord> = withContext(Dispatchers.IO) {
        if (tenantId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Tenant ID cannot be empty"))
        }

        try {
            val paymentId = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()
            val paymentData = hashMapOf<String, Any>(
                "tenantId" to tenantId,
                "amount" to amount,
                "method" to "BKASH_MANUAL",
                "status" to "PENDING",
                "referencePhone" to referencePhone,
                "notes" to notes,
                "createdAt" to createdAt
            )

            firestore.collection("subscription_payments").document(paymentId).set(paymentData).await()

            Result.success(
                SubscriptionPaymentRecord(
                    id = paymentId,
                    tenantId = tenantId,
                    amount = amount,
                    method = "BKASH_MANUAL",
                    status = "PENDING",
                    referencePhone = referencePhone,
                    createdAt = createdAt
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPaymentHistory(tenantId: String): Flow<List<SubscriptionPaymentRecord>> = callbackFlow {
        if (tenantId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("subscription_payments")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val amount = doc.getDouble("amount") ?: 250.0
                        val method = doc.getString("method") ?: "BKASH_MANUAL"
                        val status = doc.getString("status") ?: "PENDING"
                        val refPhone = doc.getString("referencePhone") ?: ""
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        SubscriptionPaymentRecord(
                            id = doc.id,
                            tenantId = tenantId,
                            amount = amount,
                            method = method,
                            status = status,
                            referencePhone = refPhone,
                            createdAt = createdAt
                        )
                    }
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }
}
