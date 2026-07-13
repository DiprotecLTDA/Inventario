package com.diprotec.inventario.data.remote.dto

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
    @Json(name = "Estado")
    val Estado: Int,

    @Json(name = "Respuesta")
    val Respuesta: String?,

    @Json(name = "Data")
    val Data: Any?,

    @Json(name = "CodigoError")
    val CodigoError: String?,

    @Json(name = "CorrelationId")
    val CorrelationId: String?
)