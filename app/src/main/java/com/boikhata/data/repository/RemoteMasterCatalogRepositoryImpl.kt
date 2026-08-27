package com.boikhata.data.repository

import com.boikhata.data.local.CloudSyncDao
import com.boikhata.data.local.MasterCatalogDao
import com.boikhata.domain.model.MasterCatalogBook
import com.boikhata.domain.repository.RemoteMasterCatalogRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteMasterCatalogRepositoryImpl @Inject constructor(
    private val masterCatalogDao: MasterCatalogDao,
    private val cloudSyncDao: CloudSyncDao
) : RemoteMasterCatalogRepository {

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override fun checkForRemoteUpdates(): Flow<Boolean> = flow {
        try {
            val syncState = cloudSyncDao.getSyncStateDirect()
            val localLatest = syncState?.lastCatalogSyncAt ?: (masterCatalogDao.getLatestMasterUpdateTimestamp() ?: 0L)

            val snapshot = firestore.collection("masterCatalog")
                .whereGreaterThan("lastUpdated", localLatest)
                .limit(1)
                .get()
                .await()

            emit(!snapshot.isEmpty)
        } catch (e: Exception) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun syncMasterCatalogFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val syncState = cloudSyncDao.getSyncStateDirect()
            val localLatest = syncState?.lastCatalogSyncAt ?: 0L

            val snapshot = firestore.collection("masterCatalog")
                .whereGreaterThan("lastUpdated", localLatest)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return@withContext Result.success(0)
            }

            val remoteBooks = snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val isbn = doc.getString("isbn")
                val titleBn = doc.getString("titleBn") ?: return@mapNotNull null
                val titleEn = doc.getString("titleEn")
                val author = doc.getString("author") ?: ""
                val publisher = doc.getString("publisher") ?: "NCTB"
                val classLevel = doc.getString("classLevel") ?: ""
                val subject = doc.getString("subject") ?: ""
                val editionYear = doc.getString("editionYear") ?: "2025"
                val mrp = doc.getDouble("mrp") ?: 0.0
                val isActive = doc.getBoolean("isActive") ?: true
                val lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()

                MasterCatalogBook(
                    id = id,
                    isbn = isbn,
                    titleBn = titleBn,
                    titleEn = titleEn,
                    author = author,
                    publisher = publisher,
                    classLevel = classLevel,
                    subject = subject,
                    editionYear = editionYear,
                    mrp = mrp,
                    isActive = isActive,
                    lastUpdated = lastUpdated
                )
            }

            if (remoteBooks.isNotEmpty()) {
                masterCatalogDao.insertAll(remoteBooks)
                val now = System.currentTimeMillis()
                cloudSyncDao.updateLastCatalogSyncAt(now)
            }

            Result.success(remoteBooks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
