package com.diprotec.inventario.core.network

import com.diprotec.inventario.core.message.AppMessages
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.Response

private data class ApiErrorResponse(
    @Json(name = "Estado") val Estado: Int?,
    @Json(name = "Respuesta") val Respuesta: String?,
    @Json(name = "CodigoError") val CodigoError: String?,
    @Json(name = "CorrelationId") val CorrelationId: String?
)

@Singleton
class ApiCallExecutor @Inject constructor(
    private val moshi: Moshi
) {
    private val errorAdapter = moshi.adapter(ApiErrorResponse::class.java)

    suspend fun <T : BaseApiResponse> execute(
        request: suspend () -> Response<T>
    ): T {
        return try {
            val response = request()

            if (!response.isSuccessful) {
                val error = runCatching {
                    response.errorBody()?.string()?.let { body ->
                        errorAdapter.fromJson(body)
                    }
                }.getOrNull()

                val message = error?.Respuesta
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: messageForHttpCode(response.code())

                throw ApiException(
                    message = message,
                    httpCode = response.code(),
                    estado = error?.Estado,
                    codigoError = error?.CodigoError,
                    correlationId = error?.CorrelationId
                )
            }

            val body = response.body()
                ?: throw ApiException(
                    message = AppMessages.Api.SIN_CONTENIDO,
                    httpCode = response.code()
                )

            if (body.apiEstado != 0 && body.apiEstado != 200) {
                val message = body.apiRespuesta
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: AppMessages.Api.GENERICO

                throw ApiException(
                    message = message,
                    httpCode = response.code(),
                    estado = body.apiEstado,
                    codigoError = body.apiCodigoError,
                    correlationId = body.apiCorrelationId
                )
            }

            body
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: UnknownHostException) {
            throw ApiException(AppMessages.Api.SIN_CONEXION, cause = e)
        } catch (e: SocketTimeoutException) {
            throw ApiException(AppMessages.Api.TIMEOUT, cause = e)
        } catch (e: IOException) {
            throw ApiException(AppMessages.Api.COMUNICACION, cause = e)
        } catch (e: Throwable) {
            throw ApiException(AppMessages.Api.GENERICO, cause = e)
        }
    }

    private fun messageForHttpCode(code: Int): String {
        return when (code) {
            400 -> AppMessages.Api.HTTP_400
            401 -> AppMessages.Api.HTTP_401
            403 -> AppMessages.Api.HTTP_403
            500 -> AppMessages.Api.HTTP_500
            in 501..599 -> AppMessages.Api.HTTP_5XX
            else -> AppMessages.Api.GENERICO
        }
    }
}
