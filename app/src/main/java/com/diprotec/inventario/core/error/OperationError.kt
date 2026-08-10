package com.diprotec.inventario.core.error

import android.util.Log
import com.diprotec.inventario.core.network.ApiException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException

private const val TAG = "OperationError"

/** Excepción cuyo mensaje ya está formateado como "Error al {acción}: {causa}", listo para UI. */
class OperationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Formatea un error en el estándar de la app: "Error al {acción}: {causa amigable}". Loguea
 * el detalle técnico (con stacktrace) y reporta el error vía [AppErrorReportingBridge] cuando
 * `cause` es un [Throwable] que no sea ya un [OperationException] (evita reportar dos veces
 * el mismo error si ya fue formateado/reportado en su origen).
 */
fun operationError(action: String, cause: Any?): String {
    val causeText = when (cause) {
        is Throwable -> cause.toUserCause()
        null -> "motivo desconocido"
        else -> cause.toString()
    }

    val message = "Error al $action: $causeText"

    if (cause is Throwable) {
        Log.e(TAG, message, cause)

        if (cause !is OperationException) {
            AppErrorReportingBridge.report(action = action, throwable = cause)
        }
    } else {
        Log.e(TAG, message)
    }

    return message
}

/** Traduce una excepción técnica a un mensaje en español apto para el usuario final. */
fun Throwable.toUserCause(): String {
    return when (this) {
        is UnknownHostException -> "sin conexión a internet"
        is SocketTimeoutException -> "el servidor tardó demasiado en responder"
        is ApiException -> httpCode?.let { code -> httpStatusMessage(code) } ?: message
        is OperationException -> message ?: "motivo desconocido"
        else -> message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }
}

private fun httpStatusMessage(code: Int): String? {
    return when (code) {
        400, 422 -> "los datos enviados son inválidos o están incompletos"
        401 -> "no fue posible autorizar la operación"
        403 -> "sin autorización para realizar esta operación"
        404 -> "recurso no encontrado"
        408 -> "el servidor tardó demasiado en responder"
        409 -> "conflicto con el estado actual"
        429 -> "demasiadas solicitudes, intente más tarde"
        in 500..599 -> "el servidor presentó un error"
        else -> null
    }
}

/**
 * Envuelve un bloque suspendido: si falla, relanza la excepción como [OperationException]
 * con el mensaje ya formateado ("Error al {action}: {causa}"), reportando el error original.
 * `CancellationException` se relanza tal cual (no se envuelve ni se reporta como error).
 */
suspend fun <T> runStep(action: String, block: suspend () -> T): T {
    try {
        return block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: OperationException) {
        throw e
    } catch (t: Throwable) {
        val message = operationError(action, t)
        throw OperationException(message, t)
    }
}
