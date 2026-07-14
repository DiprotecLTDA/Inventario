package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class ActivateDispositivoRequest(
    @Json(name = "SerialNumber")
    val SerialNumber: String,

    @Json(name = "ActivationCode")
    val ActivationCode: String,

    @Json(name = "PublicKey")
    val PublicKey: String
)

@JsonClass(generateAdapter = false)
data class ActivateDispositivoResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: Any?,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}
