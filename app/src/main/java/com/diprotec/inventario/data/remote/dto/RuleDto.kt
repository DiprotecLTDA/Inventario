package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class ReglasResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: List<ReglaDto>,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}

data class ReglaDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Nombre") val nombre: String?,
    @Json(name = "NombreApellido") val nombreApellido: String?,
    @Json(name = "Empresa") val empresa: String?,
    @Json(name = "Patente") val patente: String?,
    @Json(name = "Comentario") val comentario: String?,
    @Json(name = "Fotografia") val fotografia: String?,
    @Json(name = "EntradaSalida") val entradaSalida: String?,
    @Json(name = "ListaBlancaNegra") val listaBlancaNegra: String?,
    @Json(name = "EliminaEnviados") val eliminaEnviados: String?,
    @Json(name = "Vigente") val vigente: String?,
    @Json(name = "Perfil") val perfil: String?,
    @Json(name = "RutEmpresa") val rutEmpresa: String?
)
