package com.diprotec.inventario.core.network

class ApiException(
    override val message: String,
    val httpCode: Int? = null,
    val estado: Int? = null,
    val codigoError: String? = null,
    val correlationId: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
