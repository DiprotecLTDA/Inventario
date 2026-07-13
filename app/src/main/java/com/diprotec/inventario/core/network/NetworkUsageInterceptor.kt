package com.diprotec.inventario.core.network

import com.diprotec.inventario.data.local.dao.NetworkUsageDao
import com.diprotec.inventario.data.local.entity.NetworkUsageEntity
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

@Singleton
class NetworkUsageInterceptor @Inject constructor(
    private val dao: NetworkUsageDao
) : Interceptor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()
        val createdAt = System.currentTimeMillis()

        val endpoint = NetworkUsageClassifier.endpointFrom(request.url)
        val source = NetworkUsageClassifier.defaultSourceFor(request, endpoint)
        val operation = NetworkUsageClassifier.operationFrom(endpoint)
        val requestBytes = estimateRequestBytes(request)

        return try {
            val response = chain.proceed(request)
            val responseBytes = estimateResponseBytes(response)

            save(
                createdAt = createdAt,
                source = source,
                operation = operation,
                method = request.method,
                endpoint = endpoint,
                url = request.url.toString(),
                requestBytes = requestBytes,
                responseBytes = responseBytes,
                statusCode = response.code,
                durationMs = elapsedMillis(startedAt),
                success = response.isSuccessful,
                errorMessage = if (response.isSuccessful) null else "HTTP ${response.code}"
            )

            response
        } catch (t: Throwable) {
            save(
                createdAt = createdAt,
                source = source,
                operation = operation,
                method = request.method,
                endpoint = endpoint,
                url = request.url.toString(),
                requestBytes = requestBytes,
                responseBytes = 0L,
                statusCode = null,
                durationMs = elapsedMillis(startedAt),
                success = false,
                errorMessage = t.message
            )

            throw t
        }
    }

    private fun save(
        createdAt: Long,
        source: String,
        operation: String,
        method: String,
        endpoint: String,
        url: String,
        requestBytes: Long,
        responseBytes: Long,
        statusCode: Int?,
        durationMs: Long,
        success: Boolean,
        errorMessage: String?
    ) {
        scope.launch {
            runCatching {
                dao.insert(
                    NetworkUsageEntity(
                        createdAt = createdAt,
                        source = source,
                        operation = operation,
                        method = method,
                        endpoint = endpoint,
                        url = url,
                        requestBytes = requestBytes,
                        responseBytes = responseBytes,
                        totalBytes = requestBytes + responseBytes,
                        statusCode = statusCode,
                        durationMs = durationMs,
                        success = success,
                        errorMessage = errorMessage?.take(500)
                    )
                )
            }
        }
    }

    private fun estimateRequestBytes(request: Request): Long {
        val headerBytes = estimateHeadersBytes(request.headers)

        val bodyBytes = try {
            val length = request.body?.contentLength() ?: 0L
            if (length > 0L) length else 0L
        } catch (_: IOException) {
            0L
        } catch (_: Throwable) {
            0L
        }

        return headerBytes + bodyBytes
    }

    private fun estimateResponseBytes(response: Response): Long {
        val headerBytes = estimateHeadersBytes(response.headers)

        val bodyBytes = try {
            val length = response.body?.contentLength() ?: 0L
            if (length > 0L) length else 0L
        } catch (_: Throwable) {
            0L
        }

        return headerBytes + bodyBytes
    }

    private fun estimateHeadersBytes(headers: Headers): Long {
        val buffer = Buffer()

        for (i in 0 until headers.size) {
            buffer.writeUtf8(headers.name(i))
            buffer.writeUtf8(": ")
            buffer.writeUtf8(headers.value(i))
            buffer.writeUtf8("\r\n")
        }

        return buffer.size
    }

    private fun elapsedMillis(startedAt: Long): Long {
        return (System.nanoTime() - startedAt) / 1_000_000L
    }
}