package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class UbicacionesResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: List<UbicacionDto>,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}

data class UbicacionDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Nombre") val nombre: String?,
    @Json(name = "Vigente") val vigente: String?,
    @Json(name = "RutEmpresa") val rutEmpresa: String?
)
