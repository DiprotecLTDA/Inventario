package com.diprotec.inventario.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ApkDownloader(
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun downloadApk(
        url: String,
        fileName: String
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/octet-stream")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}")
        }

        val body = response.body
            ?: throw IllegalStateException("Respuesta sin body")

        val apkFile = File(context.getExternalFilesDir(null), fileName)

        body.byteStream().use { input ->
            FileOutputStream(apkFile).use { output ->
                input.copyTo(output)
            }
        }

        apkFile
    }

    fun buildInstallUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}