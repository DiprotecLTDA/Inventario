package com.diprotec.inventario.core.crypto

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordVerifier {
    fun verify(
        passwordIngresado: String,
        passwordHashHex: String,
        passwordSaltHex: String,
        passwordAlgoritmo: String
    ): Boolean {
        val partes = passwordAlgoritmo.split('-')
        if (partes.size != 3) return false

        val algoritmo = partes[0] + "-" + partes[1]   // p.ej. "PBKDF2-SHA256"
        val iteraciones = partes[2].toIntOrNull() ?: return false

        if (algoritmo != "PBKDF2-SHA256") {
            throw UnsupportedOperationException("Algoritmo no soportado: $algoritmo")
        }

        val saltBytes = hexToBytes(passwordSaltHex)
        val hashAlmacenado = hexToBytes(passwordHashHex)

        // Derivamos un hash del MISMO largo que el almacenado
        val hashCalculado = pbkdf2Sha256(passwordIngresado, saltBytes, iteraciones, hashAlmacenado.size)

        return constantTimeEquals(hashAlmacenado, hashCalculado)
    }

    private fun pbkdf2Sha256(
        password: String,
        salt: ByteArray,
        iteraciones: Int,
        outLenBytes: Int
    ): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iteraciones, outLenBytes * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().replace("\\s".toRegex(), "")
        require(clean.length % 2 == 0) { "Hex inválido" }
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            out[i / 2] = clean.substring(i, i + 2).toInt(16).toByte() // case-insensitive
            i += 2
        }
        return out
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}