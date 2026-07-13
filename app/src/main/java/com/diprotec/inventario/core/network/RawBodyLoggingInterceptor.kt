package com.diprotec.inventario.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

class RawBodyLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = request.body
        val buffer = Buffer()

        if (requestBody != null) {
            runCatching {
                requestBody.writeTo(buffer)
            }.onFailure {
                Log.d(TAG, "No se pudo leer request body: ${it.message}")
            }
        }

        val contentType = requestBody?.contentType()
        val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        val bytes = buffer.clone().readByteArray()

        val bodyText = runCatching {
            buffer.clone().readString(charset)
        }.getOrDefault("<non-text>")

        Log.d(TAG, "➡️ ${request.method} ${request.url}")
        Log.d(TAG, "Content-Type=$contentType len=${bytes.size}")

        if (shouldLogBody(contentType?.toString(), bytes.size)) {
            Log.d(TAG, "Body(text)=${sanitizeBody(bodyText)}")
        } else {
            Log.d(TAG, "Body(text)=omitted")
        }

        val headersText = request.headers.joinToString(separator = "\n") { header ->
            val name = header.first
            val value = header.second
            "$name: $value"
        }

        Log.d(TAG, "Headers:\n$headersText")

        return chain.proceed(request)
    }

    private fun maskHeader(name: String, value: String): String {
        return when (name.lowercase()) {
            "authorization",
            "x-api-key",
            "x-device-session",
            "x-device-signature",
            "x-device-timestamp" -> mask(value)

            else -> value
        }
    }

    private fun mask(value: String): String {
        if (value.isBlank()) return "****"

        return when {
            value.length <= 8 -> "****"

            value.startsWith("Bearer ", ignoreCase = true) -> {
                val token = value.removePrefix("Bearer ").trim()
                "Bearer ${mask(token)}"
            }

            else -> "${value.take(4)}****${value.takeLast(4)}"
        }
    }

    private fun shouldLogBody(contentType: String?, length: Int): Boolean {
        if (length == 0) return true
        if (length > MAX_BODY_LOG_BYTES) return false

        val type = contentType.orEmpty().lowercase()

        return type.contains("json") ||
                type.contains("text") ||
                type.contains("xml") ||
                type.contains("x-www-form-urlencoded")
    }

    private fun sanitizeBody(body: String): String {
        return body
            .replace(
                Regex("(?i)(\"?passwordHash\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
            .replace(
                Regex("(?i)(\"?passwordSalt\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
            .replace(
                Regex("(?i)(\"?authToken\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
            .replace(
                Regex("(?i)(\"?apiKey\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
            .replace(
                Regex("(?i)(\"?authorization\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
            .replace(
                Regex("(?i)(\"?deviceSession\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
            .replace(
                Regex("(?i)(\"?deviceSignature\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
                "$1****$3"
            )
    }

    companion object {
        private const val TAG = "RAW_HTTP"
        private const val MAX_BODY_LOG_BYTES = 4096
    }
}