package com.diprotec.inventario.core.error

/**
 * Clasificación de errores de funcionamiento de la app (no de negocio del usuario),
 * usada para el reporte técnico automático. No incluye tipos de RFID: esta app
 * (Unitech, código de barras) no tiene ese subsistema.
 */
enum class AppErrorType {
    NETWORK,
    HTTP,
    TIMEOUT,
    DNS,
    SERIALIZATION,
    API_FUNCTIONAL,
    ROOM,
    REPOSITORY,
    SERVICE,
    VIEWMODEL,
    WORKER,
    SYNC_CAPTURE,
    SYNC_FINISH,
    UNCAUGHT,
    OTHER
}

enum class ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class ErrorLocalState {
    PENDING,
    SENDING,
    SENT,
    FAILED
}

/**
 * Payload con el contexto de un error a reportar. `throwable` puede ser null cuando el
 * origen no es una excepción (p.ej. un mensaje construido a mano).
 */
data class ErrorContext(
    val action: String,
    val throwable: Throwable? = null,
    val type: AppErrorType? = null,
    val severity: ErrorSeverity? = null,
    val module: String? = null,
    val endpoint: String? = null,
    val screen: String? = null,
    val inventoryId: Long? = null,
    val workerName: String? = null,
    val extra: Map<String, String> = emptyMap()
)
