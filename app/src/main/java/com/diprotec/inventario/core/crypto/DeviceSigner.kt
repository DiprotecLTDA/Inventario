package com.diprotec.inventario.core.crypto

import android.util.Base64
import android.util.Log
import com.diprotec.inventario.core.key.DeviceKeyConstants
import com.diprotec.inventario.core.key.DeviceKeyStoreManager
import java.nio.charset.StandardCharsets
import java.security.Signature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSigner @Inject constructor(
    private val keyStoreManager: DeviceKeyStoreManager
) {

    fun signCanonicalString(canonical: String): String {
        val privateKey = keyStoreManager.getPrivateKey()

        Log.d(TAG, "alias=${keyStoreManager.getAlias()}")
        Log.d(TAG, "signatureAlgorithm=${DeviceKeyConstants.SIGNATURE_ALGORITHM}")
        Log.d(TAG, "padding=PKCS1_v1_5")
        Log.d(TAG, "canonicalToSign=${maskCanonical(canonical)}")

        val signature = Signature.getInstance(DeviceKeyConstants.SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(canonical.toByteArray(StandardCharsets.UTF_8))

        val signedBytes = signature.sign()
        val signatureBase64 = Base64.encodeToString(signedBytes, Base64.NO_WRAP)

        Log.d(TAG, "signatureBase64=${maskValue(signatureBase64)}")

        return signatureBase64
    }

    private fun maskCanonical(value: String): String {
        val parts = value.split("|")
        if (parts.size < 3) return maskValue(value)

        val method = parts[0]
        val path = parts[1]
        val timestamp = parts[2]

        return "$method|$path|${maskValue(timestamp)}"
    }

    private fun maskValue(value: String): String {
        if (value.isBlank()) return "****"
        if (value.length <= 8) return "****"

        return "${value.take(4)}****${value.takeLast(4)}"
    }

    companion object {
        private const val TAG = "DEVICE_SIGNER"
    }
}