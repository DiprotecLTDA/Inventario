package com.diprotec.inventario.data.remote.dto

data class RegistroInventarioRequest(
    val InventarioId: String,
    val Capturas: List<RegistroInventarioCapturaRequest>
)

data class RegistroInventarioCapturaRequest(
    val UbicacionId: String,
    val DispositivoId: String,
    val ProductoCodigo: String,
    val Cantidad: String,
    val UnidadMedidaId: String,
    val Fecha: String,
    val Hora: String,
    val RutUsuario: String
)

data class SendRegistroInventarioResponse(
    val Estado: Int,
    val Respuesta: String?,
    val Data: Map<String, Any?>?,
    val CodigoError: String?,
    val CorrelationId: String?
)