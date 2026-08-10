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
import com.diprotec.inventario.core.error.ErrorReportingGuard
import com.diprotec.inventario.data.local.entity.AppErrorEntity
import com.diprotec.inventario.data.repository.AppErrorRepository
import com.diprotec.inventario.service.email.MissingSmtpCredentialException
import com.diprotec.inventario.service.email.SmtpEmailSender
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.mail.AuthenticationFailedException

/**
 * Drena la cola local de errores pendientes de enviar por correo (`AppErrorEntity`,
 * `localState=PENDING`), en lotes de [BATCH_SIZE], con hasta 5 reintentos por error
 * (delegados a `AppErrorRepository.markFailed`). Envuelto en `ErrorReportingGuard` para
 * que un fallo al enviar un correo no genere, a su vez, otro reporte de error.
 */
@HiltWorker
class ErrorEmailWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppErrorRepository,
    private val emailSender: SmtpEmailSender
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork id=$id attempt=$runAttemptCount")

        return ErrorReportingGuard.withoutReporting {
            runCatching {
                processQueue(applicationContext)
            }.onFailure {
                Log.e(TAG, "Fallo procesando la cola de errores", it)
            }

            Result.success()
        }
    }

    private suspend fun processQueue(context: Context) {
        repository.recoverInterruptedSending()

        val batch = repository.claimPending(BATCH_SIZE)

        if (batch.isEmpty()) {
            runCatching { repository.purge() }
            return
        }

        repository.markSending(batch.map { it.id })

        batch.forEach { error ->
            sendOne(error)
        }

        // El lote vino lleno: probablemente queda más en la cola, se reencola a sí mismo.
        if (batch.size == BATCH_SIZE) {
            enqueue(context)
        }

        runCatching { repository.purge() }
    }

    private suspend fun sendOne(error: AppErrorEntity) {
        try {
            emailSender.send(error)
            repository.markSent(error.id)
        } catch (e: MissingSmtpCredentialException) {
            Log.w(TAG, "Sin credencial SMTP, no se reintenta", e)
            repository.markFailed(error.id, e.message, permanent = true)
        } catch (e: AuthenticationFailedException) {
            Log.w(TAG, "Autenticación SMTP rechazada, no se reintenta", e)
            repository.markFailed(error.id, e.message, permanent = true)
        } catch (t: Throwable) {
            Log.w(TAG, "Fallo enviando error id=${error.id}, se reintentará", t)
            repository.markFailed(error.id, t.message, permanent = false)
        }
    }

    companion object {
        private const val TAG = "ERROR_EMAIL"
        private const val UNIQUE_WORK = "errorEmail_drain"
        private const val BATCH_SIZE = 10

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = OneTimeWorkRequestBuilder<ErrorEmailWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                req
            )
        }
    }
}
