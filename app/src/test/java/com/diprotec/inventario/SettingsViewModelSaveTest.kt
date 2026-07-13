package com.diprotec.inventario.ui.settings



import android.content.Context
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.key.DeviceKeyStoreManager
import com.diprotec.inventario.core.key.DevicePublicKeyExporter
import com.diprotec.inventario.service.ActivateDeviceService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelSaveTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /*
     * Pruebas unitarias para SettingsViewModel.onSave().
     *
     * Este test valida el comportamiento funcional del guardado de configuración:
     *
     * 1. Rechaza Base URL vacía.
     * 2. Rechaza Base URL sin http:// o https://.
     * 3. Rechaza Base URL sin "/" final.
     * 4. Rechaza RUT Empresa vacío.
     * 5. Rechaza RUT Empresa inválido.
     * 6. Rechaza guardado cuando no existen credenciales cargadas.
     * 7. Guarda correctamente una configuración válida cuando el dispositivo ya está activado.
     * 8. Ejecuta onDone() cuando el guardado es exitoso.
     * 9. No ejecuta onDone() cuando settings.save() falla.
     * 10. Valida hasCredentials() con token y API Key presentes.
     * 11. Valida hasCredentials() cuando falta token.
     * 12. Valida hasCredentials() cuando falta API Key.
     */

    @Test
    fun onSave_sinBaseUrl_debeMostrarError() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba"
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onEmpresaChange("76001910-0")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            assertEquals("Debes ingresar la Base URL.", vm.state.value.error)
            assertFalse(onDoneCalled)
        }

    @Test
    fun onSave_baseUrlSinProtocolo_debeMostrarError() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba"
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("api.dominio.cl/")
            vm.onEmpresaChange("76001910-0")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            assertEquals(
                "La Base URL debe comenzar con http:// o https://",
                vm.state.value.error
            )
            assertFalse(onDoneCalled)
        }

    @Test
    fun onSave_baseUrlSinSlashFinal_debeMostrarError() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba"
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("https://api.dominio.cl")
            vm.onEmpresaChange("76001910-0")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            assertEquals("La Base URL debe terminar con /", vm.state.value.error)
            assertFalse(onDoneCalled)
        }

    @Test
    fun onSave_sinRutEmpresa_debeMostrarError() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba"
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("https://api.dominio.cl/")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            assertEquals("Debes ingresar el RUT de la empresa.", vm.state.value.error)
            assertFalse(onDoneCalled)
        }

    @Test
    fun onSave_rutEmpresaInvalido_debeMostrarError() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba"
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("https://api.dominio.cl/")
            vm.onEmpresaChange("76001910-1")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            assertEquals("RUT de empresa inválido", vm.state.value.error)
            assertFalse(onDoneCalled)
        }

    @Test
    fun onSave_sinCredenciales_debeMostrarError() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "",
                apiKey = ""
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("https://api.dominio.cl/")
            vm.onEmpresaChange("76001910-0")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            assertEquals("Debes cargar el archivo de credenciales.", vm.state.value.error)
            assertFalse(onDoneCalled)
        }

    @Test
    fun onSave_configuracionValida_debeGuardarYLLamarOnDone() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba",
                deviceSession = "SESSION-001",
                deviceId = "DEVICE-001",
                activationCode = "ABC123",
                deviceKeyAlias = "device_key_alias",
                deviceActivated = true
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("https://api.dominio.cl/")
            vm.onEmpresaChange("76001910-0")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            coVerify(timeout = 1000, exactly = 1) {
                settings.save(
                    baseUrl = "https://api.dominio.cl/",
                    empresaRut = "76001910-0",
                    authToken = "token-prueba",
                    apiKey = "api-key-prueba",
                    deviceSession = "SESSION-001",
                    deviceId = "DEVICE-001",
                    activationCode = "ABC123",
                    deviceKeyAlias = "device_key_alias",
                    deviceActivated = true
                )
            }

            assertEquals(null, vm.state.value.error)
            assertTrue(onDoneCalled)
        }

    @Test
    fun onSave_settingsSaveFalla_noDebeLlamarOnDone() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = mockSettingsManager(
                authToken = "token-prueba",
                apiKey = "api-key-prueba",
                deviceSession = "SESSION-001",
                deviceId = "DEVICE-001",
                activationCode = "ABC123",
                deviceKeyAlias = "device_key_alias",
                deviceActivated = true,
                saveShouldFail = true
            )

            val vm = crearViewModel(settings)

            advanceUntilIdle()

            vm.onBaseUrlChange("https://api.dominio.cl/")
            vm.onEmpresaChange("76001910-0")
            vm.onActivationCodeChange("ABC123")

            var onDoneCalled = false

            vm.onSave {
                onDoneCalled = true
            }

            advanceUntilIdle()

            coVerify(timeout = 1000, exactly = 1) {
                settings.save(
                    baseUrl = "https://api.dominio.cl/",
                    empresaRut = "76001910-0",
                    authToken = "token-prueba",
                    apiKey = "api-key-prueba",
                    deviceSession = "SESSION-001",
                    deviceId = "DEVICE-001",
                    activationCode = "ABC123",
                    deviceKeyAlias = "device_key_alias",
                    deviceActivated = true
                )
            }

            assertFalse(onDoneCalled)
        }

    @Test
    fun hasCredentials_conTokenYApiKey_debeRetornarTrue() {
        val settings = mockSettingsManager(
            authToken = "token-prueba",
            apiKey = "api-key-prueba"
        )

        val vm = crearViewModel(settings)

        assertTrue(vm.hasCredentials())
    }

    @Test
    fun hasCredentials_sinToken_debeRetornarFalse() {
        val settings = mockSettingsManager(
            authToken = "",
            apiKey = "api-key-prueba"
        )

        val vm = crearViewModel(settings)

        assertFalse(vm.hasCredentials())
    }

    @Test
    fun hasCredentials_sinApiKey_debeRetornarFalse() {
        val settings = mockSettingsManager(
            authToken = "token-prueba",
            apiKey = ""
        )

        val vm = crearViewModel(settings)

        assertFalse(vm.hasCredentials())
    }

    private fun crearViewModel(
        settings: SettingsManager
    ): SettingsViewModel {
        return SettingsViewModel(
            settings = settings,
            activateDeviceService = mockk<ActivateDeviceService>(relaxed = true),
            keyStoreManager = mockk<DeviceKeyStoreManager>(relaxed = true),
            publicKeyExporter = mockk<DevicePublicKeyExporter>(relaxed = true),
            ctx = mockk<Context>(relaxed = true)
        )
    }

    private fun mockSettingsManager(
        baseUrl: String = "",
        empresaRut: String = "",
        authToken: String = "",
        apiKey: String = "",
        deviceSession: String = "",
        deviceId: String = "",
        activationCode: String = "",
        deviceKeyAlias: String = "",
        deviceActivated: Boolean = false,
        saveShouldFail: Boolean = false
    ): SettingsManager {
        val settings = mockk<SettingsManager>(relaxed = true)

        every { settings.baseUrl } returns MutableStateFlow(baseUrl)
        every { settings.empresaRut } returns MutableStateFlow(empresaRut)
        every { settings.authToken } returns MutableStateFlow(authToken)
        every { settings.apiKey } returns MutableStateFlow(apiKey)
        every { settings.deviceSession } returns MutableStateFlow(deviceSession)
        every { settings.deviceId } returns MutableStateFlow(deviceId)
        every { settings.activationCode } returns MutableStateFlow(activationCode)
        every { settings.deviceKeyAlias } returns MutableStateFlow(deviceKeyAlias)
        every { settings.deviceActivated } returns MutableStateFlow(deviceActivated)

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
            if (saveShouldFail) {
                throw IllegalStateException("Error simulado guardando configuración")
            }
            Unit
        }

        return settings
    }
}