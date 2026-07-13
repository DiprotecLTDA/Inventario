package com.diprotec.inventario.core.key

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.File

object KeyFileReader {

    private const val TAG = "KEYFILE"

    fun readFromUri(ctx: Context, uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)

            ctx.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error leyendo archivo", t)
            null
        }
    }

    fun sanitizeToken(token: String): String {
        return token
            .trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()
    }

    fun extractAuthTokenAndApiKey(raw: String): Pair<String?, String?> {
        val lines = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var token: String? = null
        var apiKey: String? = null

        for (line in lines) {
            val parts = line.split("@@", limit = 2)
            if (parts.size != 2) continue

            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()

            when (key) {
                "authorization",
                "auth",
                "auth_token",
                "authtoken" -> {
                    token = sanitizeToken(value)
                }

                "api_key",
                "apikey",
                "x_api_key",
                "x-api-key" -> {
                    apiKey = value.trim()
                }
            }
        }

        return token to apiKey
    }

    fun extractJwtAppClaim(token: String): String? {
        return try {
            val cleanToken = sanitizeToken(token)
            val parts = cleanToken.split(".")
            if (parts.size < 2) return null

            val payloadBase64 = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let { padded ->
                    when (padded.length % 4) {
                        2 -> "$padded=="
                        3 -> "$padded="
                        else -> padded
                    }
                }

            val payloadBytes = Base64.decode(payloadBase64, Base64.DEFAULT)
            val payloadJson = String(payloadBytes, Charsets.UTF_8)
            val json = JSONObject(payloadJson)

            json.optString("app", null)
        } catch (t: Throwable) {
            Log.e(TAG, "Error leyendo claim app del JWT", t)
            null
        }
    }

    fun isInventarioToken(token: String): Boolean {
        val app = extractJwtAppClaim(token) ?: return false
        return app.equals("Inventario", ignoreCase = true)
    }

    fun existsInDownloads(fileName: String = "inventario.key"): Boolean {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val f = File(downloads, fileName)
            f.exists() && f.isFile
        } catch (t: Throwable) {
            false
        }
    }
}