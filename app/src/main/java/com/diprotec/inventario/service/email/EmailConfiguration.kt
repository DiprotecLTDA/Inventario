package com.diprotec.inventario.service.email

/** Destino fijo del reporte de errores técnicos por correo. */
data class EmailConfiguration(
    val smtpHost: String = "smtp.office365.com",
    val smtpPort: Int = 587,
    val fromAddress: String = "sai@diprotec.cl",
    val toAddress: String = "informatica@diprotec.cl"
)
