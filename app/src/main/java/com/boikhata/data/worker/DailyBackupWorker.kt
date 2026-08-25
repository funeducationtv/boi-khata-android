package com.boikhata.data.worker

import android.content.Context
import androidx.work.*
import com.boikhata.data.local.CloudSyncDao
import com.boikhata.domain.repository.BackupProgress
import com.boikhata.domain.repository.BackupRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val cloudSyncDao: CloudSyncDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        val syncState = cloudSyncDao.getSyncStateDirect() ?: return Result.success()

        val tenantId = syncState.tenantId
        if (tenantId.isBlank() || syncState.isPendingActivation) {
            return Result.success()
        }

        return try {
            val result = backupRepository.performBackup(tenantId).firstOrNull { it is BackupProgress.Success || it is BackupProgress.Error }
            if (result is BackupProgress.Success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyCloudBackupWork"

        fun scheduleDailyBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }
    }
}
