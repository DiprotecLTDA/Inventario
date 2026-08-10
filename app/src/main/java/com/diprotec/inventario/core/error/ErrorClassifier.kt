package com.diprotec.inventario.core.error

import com.diprotec.inventario.core.network.ApiException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Clasifica automáticamente el tipo y la severidad de un error para el reporte técnico. */
object ErrorClassifier {

    fun classifyType(throwable: Throwable?, moduleHint: String? = null): AppErrorType {
        val unwrapped = unwrap(throwable)

        return when {
            unwrapped is UnknownHostException -> AppErrorType.DNS
            unwrapped is SocketTimeoutException -> AppErrorType.TIMEOUT
            unwrapped is ApiException -> AppErrorType.API_FUNCTIONAL
            unwrapped is IOException -> AppErrorType.NETWORK
            unwrapped != null && isSerializationError(unwrapped) -> AppErrorType.SERIALIZATION
            unwrapped != null && isRoomError(unwrapped) -> AppErrorType.ROOM
            moduleHint?.contains("Worker", ignoreCase = true) == true -> AppErrorType.WORKER
            moduleHint?.contains("Repository", ignoreCase = true) == true -> AppErrorType.REPOSITORY
            moduleHint?.contains("ViewModel", ignoreCase = true) == true -> AppErrorType.VIEWMODEL
            moduleHint?.contains("Service", ignoreCase = true) == true -> AppErrorType.SERVICE
            else -> AppErrorType.OTHER
        }
    }

    fun classifySeverity(type: AppErrorType): ErrorSeverity {
        return when (type) {
            AppErrorType.UNCAUGHT, AppErrorType.ROOM -> ErrorSeverity.CRITICAL
            AppErrorType.NETWORK,
            AppErrorType.TIMEOUT,
            AppErrorType.DNS,
            AppErrorType.API_FUNCTIONAL,
            AppErrorType.HTTP -> ErrorSeverity.WARNING
            else -> ErrorSeverity.ERROR
        }
    }

    /**
     * Tipos de error de red/API que `AppErrorReporter` excluye del reporte: un timeout,
     * un 4xx/5xx o "sin conexión" son errores del backend/red, no del funcionamiento de
     * la app, y nunca deben generar un correo.
     */
    fun isNetworkOrApiRelated(type: AppErrorType, throwable: Throwable?): Boolean {
        if (type in NETWORK_OR_API_TYPES) return true

        val unwrapped = unwrap(throwable)
        return unwrapped is ApiException && unwrapped.httpCode != null
    }

    fun unwrap(throwable: Throwable?): Throwable? {
        var current = throwable
        var depth = 0

        while (current is OperationException && depth < MAX_UNWRAP_DEPTH) {
            current = current.cause ?: break
            depth++
        }

        return current
    }

    private fun isSerializationError(throwable: Throwable): Boolean {
        val className = throwable::class.java.name
        return className.contains("Moshi", ignoreCase = true) ||
                className.contains("Json", ignoreCase = true) ||
                className.contains("Serializ", ignoreCase = true)
    }

    private fun isRoomError(throwable: Throwable): Boolean {
        val className = throwable::class.java.name
        return className.contains("SQLite", ignoreCase = true) ||
                className.contains("Room", ignoreCase = true)
    }

    private val NETWORK_OR_API_TYPES = setOf(
        AppErrorType.NETWORK,
        AppErrorType.HTTP,
        AppErrorType.TIMEOUT,
        AppErrorType.DNS,
        AppErrorType.API_FUNCTIONAL
    )

    private const val MAX_UNWRAP_DEPTH = 8
}
