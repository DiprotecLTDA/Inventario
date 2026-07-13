package com.diprotec.inventario.core.network

import okhttp3.HttpUrl
import okhttp3.Request

object NetworkUsageClassifier {

    const val SOURCE_AUTO = "AUTO"
    const val SOURCE_MANUAL = "MANUAL"
    const val SOURCE_WORKER = "WORKER"
    const val SOURCE_UPDATE = "UPDATE"
    const val SOURCE_UNKNOWN = "UNKNOWN"

    fun endpointFrom(url: HttpUrl): String {
        val segments = url.encodedPathSegments

        return segments.lastOrNull { segment ->
            segment in knownEndpoints
        } ?: segments.lastOrNull().orEmpty().ifBlank {
            "unknown"
        }
    }

    fun operationFrom(endpoint: String): String {
        return when (endpoint) {
            "LoginDispositivo" -> "Login dispositivo"
            "ActivateDispositivo" -> "Activar dispositivo"
            "GetVersion" -> "Chequeo automático conexión API cada 15 segundos"
            "GetProductos" -> "Sincronizar productos"
            "GetUsuarios" -> "Sincronizar usuarios"
            "GetReglas" -> "Sincronizar reglas"
            "GetUbicaciones" -> "Sincronizar ubicaciones"
            "GetUnidadMedidas" -> "Sincronizar unidades"
            "GetInventarios" -> "Obtener inventarios"
            "SendRegistroInventario" -> "Enviar registro inventario"
            "FinishInventario" -> "Finalizar inventario"
            else -> "Llamada API"
        }
    }

    fun defaultSourceFor(
        request: Request,
        endpoint: String
    ): String {
        val path = request.url.encodedPath.lowercase()

        return when {
            path.endsWith(".apk") -> SOURCE_UPDATE
            endpoint == "GetVersion" -> SOURCE_AUTO
            endpoint == "SendRegistroInventario" -> SOURCE_MANUAL
            endpoint == "FinishInventario" -> SOURCE_MANUAL
            else -> SOURCE_UNKNOWN
        }
    }

    private val knownEndpoints = setOf(
        "GetUsuarios",
        "GetReglas",
        "GetUbicaciones",
        "LoginDispositivo",
        "ActivateDispositivo",
        "GetVersion",
        "GetProductos",
        "GetUnidadMedidas",
        "GetInventarios",
        "SendRegistroInventario",
        "FinishInventario"
    )
}