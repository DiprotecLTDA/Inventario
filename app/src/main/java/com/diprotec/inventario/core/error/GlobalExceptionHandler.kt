package com.diprotec.inventario.core.error

import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * `Thread.UncaughtExceptionHandler` que persiste el crash de forma síncrona (bloqueando el
 * hilo que crashea, con timeout acotado) antes de reencadenar al handler anterior del
 * sistema — nunca se lo "come": si no se reencadena, el proceso quedaría en un estado
 * indefinido en vez de terminar/reportarse normalmente. No intenta enviar el correo aquí
 * (el envío queda para el próximo arranque, vía `ErrorEmailWorker`).
 */
object GlobalExceptionHandler {

    private var installed = false

    fun install(reporter: AppErrorReporter) {
        if (installed) return
        installed = true

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                runBlocking {
                    withTimeoutOrNull(CRASH_PERSIST_TIMEOUT_MS) {
                        reporter.reportAndAwait(
                            ErrorContext(
                                action = "ejecutar la aplicación",
                                throwable = throwable,
                                type = AppErrorType.UNCAUGHT,
                                severity = ErrorSeverity.CRITICAL,
                                module = "GlobalExceptionHandler"
                            )
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "No se pudo persistir el crash", t)
            }

            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private const val TAG = "GlobalExceptionHandler"
    private const val CRASH_PERSIST_TIMEOUT_MS = 1_500L
}
