package com.diprotec.inventario.core.key

import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicePublicKeyExporter @Inject constructor(
    private val keyStoreManager: DeviceKeyStoreManager
) {

    fun exportPublicKeyPem(alias: String = keyStoreManager.getAlias()): String {
        val publicKeyBytes = keyStoreManager.getPublicKey(alias).encoded
        val base64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        val body = base64.chunked(64).joinToString("\n")

        return buildString {
            appendLine("-----BEGIN PUBLIC KEY-----")
            appendLine(body)
            append("-----END PUBLIC KEY-----")
        }
    }
}