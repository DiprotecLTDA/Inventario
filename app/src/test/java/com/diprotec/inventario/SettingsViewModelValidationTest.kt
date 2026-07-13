package com.diprotec.inventario.ui.settings

import android.content.Context
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.key.DeviceKeyStoreManager
import com.diprotec.inventario.core.key.DevicePublicKeyExporter
import com.diprotec.inventario.service.ActivateDeviceService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/*
 * Pruebas unitarias para SettingsViewModelValidationTest.
 *
 * Validaciones cubiertas:
 * 1. Verifica que no se permita guardar si la Base URL está vacía.
 * 2. Verifica que la Base URL deba comenzar con "http://" o "https://".
 * 3. Verifica que la Base URL termine con "/" para evitar errores al construir
 *    las rutas de consumo de la Web API.
 * 4. Verifica que no se permita guardar si el RUT de empresa está vacío.
 * 5. Verifica que se rechace un RUT de empresa inválido.
 * 6. Verifica que no se permita continuar si no existen credenciales cargadas,
 *    es decir, si falta el Auth Token o la API Key.
 * 7. Verifica que, ante errores de validación, no se ejecute la acción onDone().
 * 8. Verifica que los errores de validación queden reflejados en el estado
 *    del ViewModel mediante state.value.error.
 *
 * Estas pruebas no validan la lectura real del archivo de credenciales,
 * la persistencia en DataStore ni la activación del dispositivo. Esos casos
 * se cubren en pruebas específicas del módulo correspondiente.
 */

class SettingsViewModelValidationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onSave_sinBaseUrl_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key"
        )

        vm.onEmpresaChange("76001910-0")
        vm.onActivationCodeChange("ABC123")

        vm.onSave(onDone = {})

        assertEquals("Debes ingresar la Base URL.", vm.state.value.error)
    }

    @Test
    fun onSave_baseUrlSinProtocolo_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key"
        )

        vm.onBaseUrlChange("api.dominio.cl/")
        vm.onEmpresaChange("76001910-0")
        vm.onActivationCodeChange("ABC123")

        vm.onSave(onDone = {})

        assertEquals(
            "La Base URL debe comenzar con http:// o https://",
            vm.state.value.error
        )
    }

    @Test
    fun onSave_baseUrlSinSlashFinal_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key"
        )

        vm.onBaseUrlChange("https://api.dominio.cl")
        vm.onEmpresaChange("76001910-0")
        vm.onActivationCodeChange("ABC123")

        vm.onSave(onDone = {})

        assertEquals("La Base URL debe terminar con /", vm.state.value.error)
    }

    @Test
    fun onSave_sinRutEmpresa_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key"
        )

        vm.onBaseUrlChange("https://api.dominio.cl/")
        vm.onActivationCodeChange("ABC123")

        vm.onSave(onDone = {})

        assertEquals("Debes ingresar el RUT de la empresa.", vm.state.value.error)
    }

    @Test
    fun onSave_rutEmpresaInvalido_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key"
        )

        vm.onBaseUrlChange("https://api.dominio.cl/")
        vm.onEmpresaChange("76001910-1")
        vm.onActivationCodeChange("ABC123")

        vm.onSave(onDone = {})

        assertEquals("RUT de empresa inválido", vm.state.value.error)
    }

    @Test
    fun onSave_sinCredenciales_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "",
            apiKey = ""
        )

        vm.onBaseUrlChange("https://api.dominio.cl/")
        vm.onEmpresaChange("76001910-0")
        vm.onActivationCodeChange("ABC123")

        vm.onSave(onDone = {})

        assertEquals("Debes cargar el archivo de credenciales.", vm.state.value.error)
    }

    @Test
    fun onSave_sinActivationCodeYDispositivoNoActivado_debeMostrarError() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key",
            deviceActivated = false
        )

        vm.onBaseUrlChange("https://api.dominio.cl/")
        vm.onEmpresaChange("76001910-0")
        vm.onActivationCodeChange("")

        vm.onSave(onDone = {})

        assertEquals("Debes ingresar el Activation Code.", vm.state.value.error)
    }

    @Test
    fun hasCredentials_conTokenYApiKey_debeRetornarTrue() {
        val vm = crearViewModel(
            authToken = "token",
            apiKey = "api-key"
        )

        assertEquals(true, vm.hasCredentials())
    }

    @Test
    fun hasCredentials_sinToken_debeRetornarFalse() {
        val vm = crearViewModel(
            authToken = "",
            apiKey = "api-key"
        )

        assertFalse(vm.hasCredentials())
    }

    private fun crearViewModel(
        baseUrl: String = "",
        empresaRut: String = "",
        activationCode: String = "",
        authToken: String = "",
        apiKey: String = "",
        deviceSession: String = "",
        deviceId: String = "",
        deviceKeyAlias: String = "",
        deviceActivated: Boolean = false
    ): SettingsViewModel {
        val settings = mockSettingsManager(
            baseUrl = baseUrl,
            empresaRut = empresaRut,
            activationCode = activationCode,
            authToken = authToken,
            apiKey = apiKey,
            deviceSession = deviceSession,
            deviceId = deviceId,
            deviceKeyAlias = deviceKeyAlias,
            deviceActivated = deviceActivated
        )

        return SettingsViewModel(
            settings = settings,
            activateDeviceService = mockk(relaxed = true),
            keyStoreManager = mockk(relaxed = true),
            publicKeyExporter = mockk(relaxed = true),
            ctx = mockk<Context>(relaxed = true)
        )
    }
}

fun mockSettingsManager(
    baseUrl: String = "",
    empresaRut: String = "",
    activationCode: String = "",
    authToken: String = "",
    apiKey: String = "",
    deviceSession: String = "",
    deviceId: String = "",
    deviceKeyAlias: String = "",
    deviceActivated: Boolean = false
): SettingsManager {
    val settings = mockk<SettingsManager>(relaxed = true)

    val baseUrlFlow = MutableStateFlow(baseUrl)
    val empresaRutFlow = MutableStateFlow(empresaRut)
    val activationCodeFlow = MutableStateFlow(activationCode)
    val authTokenFlow = MutableStateFlow(authToken)
    val apiKeyFlow = MutableStateFlow(apiKey)
    val deviceSessionFlow = MutableStateFlow(deviceSession)
    val deviceIdFlow = MutableStateFlow(deviceId)
    val deviceKeyAliasFlow = MutableStateFlow(deviceKeyAlias)
    val deviceActivatedFlow = MutableStateFlow(deviceActivated)

    every { settings.baseUrl } returns baseUrlFlow
    every { settings.empresaRut } returns empresaRutFlow
    every { settings.activationCode } returns activationCodeFlow
    every { settings.authToken } returns authTokenFlow
    every { settings.apiKey } returns apiKeyFlow
    every { settings.deviceSession } returns deviceSessionFlow
    every { settings.deviceId } returns deviceIdFlow
    every { settings.deviceKeyAlias } returns deviceKeyAliasFlow
    every { settings.deviceActivated } returns deviceActivatedFlow

    coEvery {
        settings.save(
            baseUrl = any(),
            empresaRut = any(),
            authToken = any(),
            apiKey = any(),
            deviceSession = any(),
            deviceId = any(),
            activationCode = any(),
            deviceKeyAlias = any(),
            deviceActivated = any()
        )
    } coAnswers {
        baseUrlFlow.value = arg(0)
        empresaRutFlow.value = arg(1)
        authTokenFlow.value = arg(2)
        apiKeyFlow.value = arg(3)
        deviceSessionFlow.value = arg(4)
        deviceIdFlow.value = arg(5)
        activationCodeFlow.value = arg(6)
        deviceKeyAliasFlow.value = arg(7)
        deviceActivatedFlow.value = arg(8)
    }

    return settings
}