package com.diprotec.inventario.data.remote.dto

import com.squareup.moshi.Json

data class ProductosResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: List<ProductoDto>,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
)

data class ProductoDto(
    @Json(name = "Codigo") val codigo: String,
    @Json(name = "CodigoSecundario") val codigoSecundario: String?,
    @Json(name = "Descripcion") val descripcion: String?,
    @Json(name = "Vigente") val vigente: String?,
    @Json(name = "RutEmpresa") val rutEmpresa: String?
)