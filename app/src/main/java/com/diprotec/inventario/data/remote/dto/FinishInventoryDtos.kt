package com.diprotec.inventario.data.remote.dto

data class FinalizarInventarioRequest(
    val InventarioId: Long,
    val UsuarioRUT: String
)

data class FinalizarInventarioResponse(
    val Estado: Int,
    val Respuesta: String?,
    val Data: Map<String, Any?>?,
    val CodigoError: String?,
    val CorrelationId: String?
)