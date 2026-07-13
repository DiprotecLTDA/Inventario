package com.diprotec.inventario.data.remote.dto

data class LoginDispositivoResponse(
    val Estado: Int,
    val Respuesta: String?,
    val Data: String?,
    val CodigoError: String?,
    val CorrelationId: String?
)