package com.diprotec.inventario.data.local.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith


/*
 * Pruebas instrumentadas para SettingsDataStore.
 * Validaciones realizadas:
 * 1. Guarda y recupera correctamente una configuración completa, incluyendo:
 *    - Base URL.
 *    - RUT de empresa.
 *    - Auth Token.
 *    - API Key.
 *    - Device Session.
 *    - Device ID.
 *    - Activation Code.
 *    - Alias de llave del dispositivo.
 *    - Estado de dispositivo activado.
 *
 * 2. Normaliza la Base URL agregando "/" al final cuando se guarda una URL
 *    válida sin slash final.
 *
 * 3. Guarda y recupera correctamente los datos específicos de activación:
 *    - Activation Code.
 *    - Device Key Alias.
 *    - Device Activated.
 * Estas pruebas confirman que la configuración inicial y los datos de activación
 * quedan persistidos correctamente en SettingsDataStore.
 */

@RunWith(AndroidJUnit4::class)
class SettingsDataStoreInstrumentedTest {

    @Test
    fun save_configuracionCompleta_debePersistirValores() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        store.save(
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

        val settings = store.settingsFlow.first()

        assertEquals("https://api.dominio.cl/", settings.baseUrl)
        assertEquals("76001910-0", settings.empresaRut)
        assertEquals("token-prueba", settings.authToken)
        assertEquals("api-key-prueba", settings.apiKey)
        assertEquals("SESSION-001", settings.deviceSession)
        assertEquals("DEVICE-001", settings.deviceId)
        assertEquals("ABC123", settings.activationCode)
        assertEquals("device_key_alias", settings.deviceKeyAlias)
        assertTrue(settings.deviceActivated)
    }

    @Test
    fun save_baseUrlSinSlash_debeNormalizarConSlashFinal() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        store.save(
            baseUrl = "https://api.dominio.cl",
            empresaRut = "76001910-0",
            authToken = "token-prueba",
            apiKey = "api-key-prueba",
            deviceSession = "",
            deviceId = "",
            activationCode = "",
            deviceKeyAlias = "",
            deviceActivated = false
        )

        val settings = store.settingsFlow.first()

        assertEquals("https://api.dominio.cl/", settings.baseUrl)
    }

    @Test
    fun saveActivation_debePersistirDatosDeActivacion() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsDataStore(context)

        store.saveActivation(
            activationCode = "ABC123",
            deviceKeyAlias = "device_key_alias",
            deviceActivated = true
        )

        val settings = store.settingsFlow.first()

        assertEquals("ABC123", settings.activationCode)
        assertEquals("device_key_alias", settings.deviceKeyAlias)
        assertTrue(settings.deviceActivated)
    }
}