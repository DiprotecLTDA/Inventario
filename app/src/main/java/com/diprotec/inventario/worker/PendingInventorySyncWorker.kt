package com.diprotec.inventario.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.diprotec.inventario.core.network.NetworkUsageClassifier
import com.diprotec.inventario.core.network.NetworkUsageContext
import com.diprotec.inventario.service.SyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class PendingInventorySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork id=$id attempt=$runAttemptCount")

        return runSyncWork(
            tag = TAG,
            onUnexpected = { t ->
                Log.e(TAG, "Unexpected error -> retry", t)
                Result.retry()
            }
        ) {
            val summary = NetworkUsageContext.runWith(
                source = NetworkUsageClassifier.SOURCE_WORKER,
                operation = "Worker sincronizar inventarios pendientes"
            ) {
                sync.syncAllInventarioPendiente()
            }

            Log.d(
                TAG,
                "Pending inventory sync OK. Capturas=${summary.capturas}, Finalizados=${summary.finalizados}"
            )
        }
    }

    companion object {
        private const val TAG = "PENDING_INV_SYNC"
        private const val UNIQUE_PERIODIC = "pendingInventorySync_periodic"
        private const val UNIQUE_ONCE = "pendingInventorySync_once"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = PeriodicWorkRequestBuilder<PendingInventorySyncWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        fun runOnce(context: Context): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = OneTimeWorkRequestBuilder<PendingInventorySyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONCE,
                ExistingWorkPolicy.REPLACE,
                req
            )

            return req.id
        }
    }
}
