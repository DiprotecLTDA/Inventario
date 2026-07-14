package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class InventariosResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: List<InventarioDto>,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}

data class InventarioDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Descripcion") val descripcion: String?,
    @Json(name = "Fecha") val fecha: String?,
    @Json(name = "Hora") val hora: String?,
    @Json(name = "Desde") val desde: String?,
    @Json(name = "Hasta") val hasta: String?,
    @Json(name = "RutAdministrador") val rutAdministrador: String?,
    @Json(name = "Vigente") val vigente: String?,
    @Json(name = "RutEmpresa") val rutEmpresa: String?,
    @Json(name = "Usuarios") val usuarios: List<InventarioUsuarioDto>?
)

data class InventarioUsuarioDto(

    @Json(name = "InventarioId") val inventarioId: String?,
    @Json(name = "RutUsuario") val rutUsuario: String?
)
