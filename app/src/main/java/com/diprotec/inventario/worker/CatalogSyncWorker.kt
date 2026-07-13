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
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class CatalogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork id=$id attempt=$runAttemptCount")

        return try {
            val summary = NetworkUsageContext.runWith(
                source = NetworkUsageClassifier.SOURCE_WORKER,
                operation = "Worker sincronizar catálogos"
            ) {
                sync.syncAllCatalogs()
            }

            Log.d(TAG, "Catalog sync OK: $summary")

            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Network error -> retry", e)
            Result.retry()
        } catch (e: HttpException) {
            Log.w(
                TAG,
                "HTTP ${e.code()} -> ${if (e.code() >= 500) "retry" else "failure"}",
                e
            )

            if (e.code() >= 500) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error -> failure", t)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "CATALOG_SYNC"
        private const val UNIQUE_PERIODIC = "catalogSync_periodic"
        private const val UNIQUE_ONCE = "catalogSync_once"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = PeriodicWorkRequestBuilder<CatalogSyncWorker>(
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

        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONCE,
                ExistingWorkPolicy.KEEP,
                req
            )
        }
    }
}