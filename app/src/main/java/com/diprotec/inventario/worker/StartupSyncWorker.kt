package com.diprotec.inventario.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
class StartupSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork id=$id attempt=$runAttemptCount")

        return try {
            val versionResult = NetworkUsageContext.runWith(
                source = NetworkUsageClassifier.SOURCE_WORKER,
                operation = "Worker chequeo actualización inicial"
            ) {
                sync.checkStartupUpdateAndSavePending()
            }

            val users = NetworkUsageContext.runWith(
                source = NetworkUsageClassifier.SOURCE_WORKER,
                operation = "Worker sincronizar usuarios inicial"
            ) {
                sync.syncUsers()
            }

            Log.d(
                TAG,
                "Startup sync OK. version=${versionResult.versionName ?: "-"}, " +
                        "hasNewVersion=${versionResult.hasNewVersion}, " +
                        "mandatory=${versionResult.mandatory}, " +
                        "users=$users"
            )

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
        private const val TAG = "STARTUP_SYNC"
        private const val UNIQUE_ONCE = "startupSync_once"

        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = OneTimeWorkRequestBuilder<StartupSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS)
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