package com.diprotec.inventario.core.error

import android.content.Context
import android.os.Build
import android.util.Log
import com.diprotec.inventario.core.device.GetSerialNumber
import com.diprotec.inventario.data.local.entity.AppErrorEntity
import com.diprotec.inventario.data.repository.AppErrorRepository
import com.diprotec.inventario.ui.main.CurrentScreenTracker
import com.diprotec.inventario.worker.ErrorEmailWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto único de reporte de errores de funcionamiento de la app (no de red/API: esos se
 * excluyen a propósito, ver [ErrorClassifier.isNetworkOrApiRelated]). Persiste el error en
 * Room y agenda su envío por correo vía [ErrorEmailWorker].
 */
@Singleton
class AppErrorReporter @Inject constructor(
    private val repository: AppErrorRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        AppErrorReportingBridge.attach(this)
    }

    /**
     * No es suspend a propósito: se llama desde código síncrono (p.ej. `operationError()`)
     * y nunca debe bloquear ni propagar excepciones al llamador. El guardado es
     * fire-and-forget en un scope propio.
     */
    fun report(errorContext: ErrorContext) {
        scope.launch {
            reportAndAwait(errorContext)
        }
    }

    /**
     * Variante que SÍ espera a que termine de persistir. La usa [GlobalExceptionHandler]
     * para poder bloquear el hilo que está crasheando hasta guardar el error (con timeout),
     * ya que un `report()` fire-and-forget normalmente no alcanza a completarse antes de que
     * el proceso muera.
     */
    suspend fun reportAndAwait(errorContext: ErrorContext) {
        if (ErrorReportingGuard.isSuppressed()) return

        runCatching {
            persistAndSchedule(errorContext)
        }.onFailure {
            Log.e(TAG, "No se pudo persistir el reporte de error", it)
        }
    }

    private suspend fun persistAndSchedule(errorContext: ErrorContext) {
        val moduleHint = errorContext.module ?: errorContext.action
        val type = errorContext.type
            ?: ErrorClassifier.classifyType(errorContext.throwable, moduleHint)

        // Política de exclusión: solo errores de funcionamiento de la app, nunca de la API/red.
        // Un crash real (UNCAUGHT) nunca se filtra: si llegó a tumbar la app, algo no lo
        // manejó, y eso es en sí mismo un error de funcionamiento, sin importar la causa raíz.
        if (type != AppErrorType.UNCAUGHT &&
            ErrorClassifier.isNetworkOrApiRelated(type, errorContext.throwable)
        ) {
            return
        }

        val severity = errorContext.severity ?: ErrorClassifier.classifySeverity(type)
        val serial = runCatching { GetSerialNumber(context).serial }.getOrNull()
        val now = System.currentTimeMillis()

        val exceptionClassName = ErrorClassifier.unwrap(errorContext.throwable)
            ?.let { it::class.java.name }

        val fingerprint = ErrorFingerprintGenerator.generate(
            type = type,
            module = errorContext.module,
            exceptionClassName = exceptionClassName,
            endpoint = errorContext.endpoint,
            inventoryId = errorContext.inventoryId,
            serial = serial
        )

        val stackTrace = errorContext.throwable?.let { throwable ->
            val chain = ExceptionChainFormatter.format(throwable)
            val fullTrace = throwable.stackTraceToString()
            ErrorSanitizer.sanitizeStackTrace("$chain\n$fullTrace")
        }

        val entity = AppErrorEntity(
            fingerprint = fingerprint,
            localState = ErrorLocalState.PENDING.name,
            createdAt = now,
            lastSeenAt = now,
            type = type.name,
            severity = severity.name,
            module = errorContext.module,
            action = errorContext.action,
            message = ErrorSanitizer.sanitizeForLog(
                errorContext.throwable?.message ?: errorContext.action
            ),
            stackTrace = stackTrace,
            endpoint = errorContext.endpoint,
            screen = errorContext.screen ?: CurrentScreenTracker.currentScreen.value,
            inventoryId = errorContext.inventoryId,
            workerName = errorContext.workerName,
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceSerial = ErrorSanitizer.maskIdentifier(serial),
            appVersionName = currentAppVersionName(),
            appVersionCode = currentAppVersionCode()
        )

        repository.insert(entity)

        ErrorEmailWorker.enqueue(context)
    }

    private fun currentAppVersionName(): String? {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    private fun currentAppVersionCode(): Long? {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "AppErrorReporter"
    }
}
