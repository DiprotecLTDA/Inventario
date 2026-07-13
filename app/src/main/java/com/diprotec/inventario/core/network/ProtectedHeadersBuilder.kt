package com.diprotec.inventario.core.network

import android.util.Log
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.crypto.DeviceCanonicalStringBuilder
import com.diprotec.inventario.core.crypto.DeviceSigner
import com.diprotec.inventario.core.crypto.DeviceTimestampProvider
import javax.inject.Inject
import javax.inject.Singleton

data class ProtectedHeaders(
    val apiKey: String,
    val authorization: String,
    val deviceSession: String,
    val deviceSignature: String,
    val deviceTimestamp: String
)

@Singleton
class ProtectedHeadersBuilder @Inject constructor(
    private val settings: SettingsManager,
    private val timestampProvider: DeviceTimestampProvider,
    private val canonicalStringBuilder: DeviceCanonicalStringBuilder,
    private val deviceSigner: DeviceSigner
) {

    fun build(
        method: String,
        relativeUrl: String
    ): ProtectedHeaders {
        val apiKey = settings.apiKey.value.trim()
        require(apiKey.isNotBlank()) { "X-API-KEY no configurada" }

        val token = settings.authToken.value.trim()
        require(token.isNotBlank()) { "Authorization no configurado" }

        val deviceSession = settings.deviceSession.value.trim()
        require(deviceSession.isNotBlank()) { "X-DEVICE-SESSION no configurado" }

        val timestamp = timestampProvider.nowUtc()

        val canonical = canonicalStringBuilder.build(
            method = method,
            relativeUrl = relativeUrl,
            timestamp = timestamp
        )

        val signature = deviceSigner.signCanonicalString(canonical)

        val deviceTimestamp = timestamp

        check(timestamp == deviceTimestamp) {
            "timestamp y deviceTimestamp no coinciden"
        }

        Log.d(TAG, "======== PRE REQUEST SIGN DEBUG ========")
        Log.d(TAG, "method=$method")
        Log.d(TAG, "relativeUrl=$relativeUrl")
        Log.d(TAG, "timestamp=${maskValue(timestamp)}")
        Log.d(TAG, "deviceTimestamp=${maskValue(deviceTimestamp)}")
        Log.d(TAG, "timestampEqualsDeviceTimestamp=${timestamp == deviceTimestamp}")
        Log.d(TAG, "canonical=${maskCanonical(canonical)}")
        Log.d(TAG, "signature=${maskValue(signature)}")
        Log.d(TAG, "signatureAlgorithm=SHA256withRSA")
        Log.d(TAG, "padding=PKCS1_v1_5")
        Log.d(TAG, "encoding=UTF-8")
        Log.d(TAG, "======== END SIGN DEBUG ========")

        return ProtectedHeaders(
            apiKey = apiKey,
            authorization = "Bearer $token",
            deviceSession = deviceSession,
            deviceSignature = signature,
            deviceTimestamp = deviceTimestamp
        )
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
        private const val TAG = "DEVICE_SIGN"
    }
}