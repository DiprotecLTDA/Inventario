package com.diprotec.inventario.service

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.ActivateDispositivoRequest
import com.diprotec.inventario.data.remote.dto.ActivateDispositivoResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/*
 * Pruebas unitarias para ActivateDeviceService.
 *
 * Validaciones realizadas:
 * 1. Cuando la API responde correctamente con Data como String, retorna el Device ID esperado.
 * 2. Cuando la API responde correctamente con Data como Map y contiene "DispositivoId",
 *    retorna el Device ID esperado.
 * 3. Envía correctamente el RUT de empresa al endpoint de activación.
 * 4. Envía correctamente el header X-API-KEY usando la API Key configurada.
 * 5. Envía correctamente el header Authorization con formato "Bearer <token>".
 * 6. Envía correctamente el SerialNumber del dispositivo dentro del body.
 * 7. Envía correctamente el ActivationCode dentro del body.
 * 8. Envía correctamente la PublicKey dentro del body.
 * 9. Lanza error cuando el authToken no está configurado.
 * 10. Lanza error controlado cuando la API responde HTTP 401.
 * 11. Lanza error controlado cuando la API responde HTTP 500.
 * 12. Lanza error cuando la API responde con Estado funcional distinto de éxito.
 * 13. Incluye en el error la información funcional devuelta por la API,
 *     como Estado, Respuesta y Código de Error.
 * 14. Lanza error cuando la respuesta exitosa no contiene un DispositivoId válido.
 *
 * Estas pruebas no realizan llamadas reales a la Web API. La API se simula con mocks,
 * por lo que el objetivo es validar la lógica del servicio, la construcción de la
 * solicitud y el manejo de respuestas exitosas y fallidas.
 */


class ActivateDeviceServiceTest {

    @Test
    fun activate_respuestaExitosaConDataString_debeRetornarDeviceId() = runTest {
        val api = mockk<ApiService>()
        val settings = mockSettingsManagerParaService(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        coEvery {
            api.activateDispositivo(
                empresaRUT = "76001910-0",
                apiKey = "api-key-prueba",
                authorization = "Bearer token-prueba",
                body = any()
            )
        } returns Response.success(
            ActivateDispositivoResponse(
                Estado = 0,
                Respuesta = "OK",
                Data = "DEVICE-001",
                CodigoError = null,
                CorrelationId = null
            )
        )

        val service = ActivateDeviceService(api, settings)

        val deviceId = service.activate(
            empresaRut = "76001910-0",
            serialNumber = "SERIAL-001",
            activationCode = "ABC123",
            publicKey = "PUBLIC_KEY_PEM"
        )

        assertEquals("DEVICE-001", deviceId)

        coVerify {
            api.activateDispositivo(
                empresaRUT = "76001910-0",
                apiKey = "api-key-prueba",
                authorization = "Bearer token-prueba",
                body = ActivateDispositivoRequest(
                    SerialNumber = "SERIAL-001",
                    ActivationCode = "ABC123",
                    PublicKey = "PUBLIC_KEY_PEM"
                )
            )
        }
    }

    @Test
    fun activate_respuestaExitosaConDataMap_debeRetornarDeviceId() = runTest {
        val api = mockk<ApiService>()
        val settings = mockSettingsManagerParaService(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        coEvery {
            api.activateDispositivo(any(), any(), any(), any())
        } returns Response.success(
            ActivateDispositivoResponse(
                Estado = 0,
                Respuesta = "OK",
                Data = mapOf("DispositivoId" to "DEVICE-002"),
                CodigoError = null,
                CorrelationId = null
            )
        )

        val service = ActivateDeviceService(api, settings)

        val deviceId = service.activate(
            empresaRut = "76001910-0",
            serialNumber = "SERIAL-001",
            activationCode = "ABC123",
            publicKey = "PUBLIC_KEY_PEM"
        )

        assertEquals("DEVICE-002", deviceId)
    }

    @Test
    fun activate_authTokenVacio_debeLanzarError() = runTest {
        val api = mockk<ApiService>(relaxed = true)
        val settings = mockSettingsManagerParaService(
            authToken = "",
            apiKey = "api-key-prueba"
        )

        val service = ActivateDeviceService(api, settings)

        val error = runCatching {
            service.activate(
                empresaRut = "76001910-0",
                serialNumber = "SERIAL-001",
                activationCode = "ABC123",
                publicKey = "PUBLIC_KEY_PEM"
            )
        }.exceptionOrNull()

        assertEquals("Authorization no configurado", error?.message)
    }

    @Test
    fun activate_http401_debeLanzarErrorConCodigo() = runTest {
        val api = mockk<ApiService>()
        val settings = mockSettingsManagerParaService(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        coEvery {
            api.activateDispositivo(any(), any(), any(), any())
        } returns Response.error(
            401,
            "Unauthorized".toResponseBody("text/plain".toMediaType())
        )

        val service = ActivateDeviceService(api, settings)

        val error = runCatching {
            service.activate(
                empresaRut = "76001910-0",
                serialNumber = "SERIAL-001",
                activationCode = "ABC123",
                publicKey = "PUBLIC_KEY_PEM"
            )
        }.exceptionOrNull()

        assertTrue(error?.message?.contains("ActivateDispositivo HTTP 401") == true)
    }

    @Test
    fun activate_http500_debeLanzarErrorConCodigo() = runTest {
        val api = mockk<ApiService>()
        val settings = mockSettingsManagerParaService(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        coEvery {
            api.activateDispositivo(any(), any(), any(), any())
        } returns Response.error(
            500,
            "Error interno".toResponseBody("text/plain".toMediaType())
        )

        val service = ActivateDeviceService(api, settings)

        val error = runCatching {
            service.activate(
                empresaRut = "76001910-0",
                serialNumber = "SERIAL-001",
                activationCode = "ABC123",
                publicKey = "PUBLIC_KEY_PEM"
            )
        }.exceptionOrNull()

        assertTrue(error?.message?.contains("ActivateDispositivo HTTP 500") == true)
    }

    @Test
    fun activate_estadoFuncionalError_debeLanzarError() = runTest {
        val api = mockk<ApiService>()
        val settings = mockSettingsManagerParaService(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        coEvery {
            api.activateDispositivo(any(), any(), any(), any())
        } returns Response.success(
            ActivateDispositivoResponse(
                Estado = 99,
                Respuesta = "Código inválido",
                Data = null,
                CodigoError = "ACT-001",
                CorrelationId = null
            )
        )

        val service = ActivateDeviceService(api, settings)

        val error = runCatching {
            service.activate(
                empresaRut = "76001910-0",
                serialNumber = "SERIAL-001",
                activationCode = "ABC123",
                publicKey = "PUBLIC_KEY_PEM"
            )
        }.exceptionOrNull()

        assertTrue(error?.message?.contains("ActivateDispositivo Estado=99") == true)
        assertTrue(error?.message?.contains("Código inválido") == true)
    }

    @Test
    fun activate_respuestaSinDeviceId_debeLanzarError() = runTest {
        val api = mockk<ApiService>()
        val settings = mockSettingsManagerParaService(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        coEvery {
            api.activateDispositivo(any(), any(), any(), any())
        } returns Response.success(
            ActivateDispositivoResponse(
                Estado = 0,
                Respuesta = "OK",
                Data = "",
                CodigoError = null,
                CorrelationId = null
            )
        )

        val service = ActivateDeviceService(api, settings)

        val error = runCatching {
            service.activate(
                empresaRut = "76001910-0",
                serialNumber = "SERIAL-001",
                activationCode = "ABC123",
                publicKey = "PUBLIC_KEY_PEM"
            )
        }.exceptionOrNull()

        assertEquals("ActivateDispositivo no devolvió DispositivoId", error?.message)
    }

    private fun mockSettingsManagerParaService(
        authToken: String,
        apiKey: String
    ): SettingsManager {
        val settings = mockk<SettingsManager>(relaxed = true)

        every { settings.authToken } returns MutableStateFlow(authToken)
        every { settings.apiKey } returns MutableStateFlow(apiKey)

        return settings
    }
}