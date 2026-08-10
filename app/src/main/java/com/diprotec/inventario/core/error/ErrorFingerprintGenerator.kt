package com.diprotec.inventario.core.error

import java.security.MessageDigest

/**
 * Genera un fingerprint SHA-256 estable a partir de las señales que identifican "el mismo"
 * error (tipo + módulo + clase de excepción + endpoint + inventario + serie del equipo).
 * Se usa para poder agrupar/priorizar en el futuro, aunque hoy la deduplicación de envío
 * esté deliberadamente deshabilitada (cada error genera su propio registro y su propio correo).
 */
object ErrorFingerprintGenerator {

    fun generate(
        type: AppErrorType,
        module: String?,
        exceptionClassName: String?,
        endpoint: String?,
        inventoryId: Long?,
        serial: String?
    ): String {
        val raw = listOf(
            type.name,
            module.orEmpty(),
            exceptionClassName.orEmpty(),
            endpoint.orEmpty(),
            inventoryId?.toString().orEmpty(),
            serial.orEmpty()
        ).joinToString("|")

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))

        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
