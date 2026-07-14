package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json

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
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: Map<String, Any?>?,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}
