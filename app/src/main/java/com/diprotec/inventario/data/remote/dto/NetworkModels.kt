package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class UsersResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String,
    @Json(name = "Data") val data: List<UserDto>,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta: String? get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}

data class UserDto(
    @Json(name = "Rut") val rut: String,
    @Json(name = "Nombre") val nombre: String,
    @Json(name = "Email") val email: String?,
    @Json(name = "Telefono") val telefono: String?,
    @Json(name = "Perfil") val perfil: String?,
    @Json(name = "PerfilId") val perfilId: Int?,                 // NUEVO
    @Json(name = "PasswordHash") val passwordHash: String?,      // NUEVO
    @Json(name = "PasswordSalt") val passwordSalt: String?,      // NUEVO
    @Json(name = "PasswordAlgoritmo") val passwordAlgoritmo: String?, // NUEVO
    @Json(name = "Vigente") val vigente: String?                 // "0" o "1"
)
