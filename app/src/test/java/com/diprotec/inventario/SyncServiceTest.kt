package com.diprotec.inventario.service

import android.content.Context
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.repository.InventoryRemoteRepository
import com.diprotec.inventario.data.repository.InventoryRepository
import com.diprotec.inventario.data.repository.LocationRepository
import com.diprotec.inventario.data.repository.ProductRepository
import com.diprotec.inventario.data.repository.RuleRepository
import com.diprotec.inventario.data.repository.SyncLogRepository
import com.diprotec.inventario.data.repository.UnitMeasureRepository
import com.diprotec.inventario.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncServiceTest {

    /*
     * Pruebas unitarias para SyncService.
     *
     * Este test valida el comportamiento base del módulo de sincronización:
     *
     * 1. Rechaza sincronización si no existe RUT de empresa configurado.
     * 2. Rechaza sincronización si no existe API Key configurada.
     * 3. Rechaza sincronización si no existe Authorization Token configurado.
     * 4. Rechaza sincronización si el dispositivo no está activado.
     * 5. Usa una sesión de dispositivo existente sin llamar nuevamente a loginDispositivo.
     * 6. Sincroniza usuarios cuando la API/local repository retorna una lista vacía.
     * 7. Sincroniza reglas cuando la API/local repository retorna una lista vacía.
     * 8. Sincroniza ubicaciones cuando la API/local repository retorna una lista vacía.
     * 9. Sincroniza productos cuando la API/local repository retorna una lista vacía.
     * 10. Sincroniza unidades de medida cuando la API/local repository retorna una lista vacía.
     * 11. Sincroniza inventarios remotos cuando la API/local repository retorna una lista vacía.
     * 12. Ejecuta syncAllCatalogs() y retorna resumen con contadores en cero cuando no hay datos.
     * 13. Ejecuta syncAllInventarioPendiente() y retorna capturas/finalizados en cero cuando no hay pendientes.
     *
     * Estas pruebas no validan todavía el mapeo de DTOs a entidades ni el envío real
     * de capturas/finalización de inventario. Esos casos deben probarse en tests
     * específicos de inventarios pendientes y actualización remota.
     */

    @Test
    fun syncUsers_sinEmpresaRut_debeRetornarError() = runTest {
        val deps = createDeps(
            empresaRut = "",
            apiKey = "api-key-prueba",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = true
        )

        val error = runCatching {
            deps.service.syncUsers()
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Empresa RUT no configurado", error?.message)
    }

    @Test
    fun syncUsers_sinApiKey_debeRetornarError() = runTest {
        val deps = createDeps(
            empresaRut = "76001910-0",
            apiKey = "",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = true
        )

        val error = runCatching {
            deps.service.syncUsers()
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("X-API-KEY no configurada", error?.message)
    }

    @Test
    fun syncUsers_sinAuthToken_debeRetornarError() = runTest {
        val deps = createDeps(
            empresaRut = "76001910-0",
            apiKey = "api-key-prueba",
            authToken = "",
            deviceSession = "SESSION-001",
            deviceActivated = true
        )

        val error = runCatching {
            deps.service.syncUsers()
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Authorization no configurado", error?.message)
    }

    @Test
    fun syncUsers_dispositivoNoActivado_debeRetornarError() = runTest {
        val deps = createDeps(
            empresaRut = "76001910-0",
            apiKey = "api-key-prueba",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = false
        )

        val error = runCatching {
            deps.service.syncUsers()
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("El dispositivo no está activado", error?.message)
    }

    @Test
    fun warmUpDeviceSession_conSesionExistente_debeRetornarSesionSinLlamarApi() = runTest {
        val deps = createDeps(
            empresaRut = "76001910-0",
            apiKey = "api-key-prueba",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = true
        )

        val result = deps.service.warmUpDeviceSession()

        assertEquals("SESSION-001", result)

        coVerify(exactly = 0) {
            deps.api.loginDispositivo(any(), any(), any(), any())
        }
    }

    @Test
    fun syncUsers_configValida_sinUsuarios_debeGuardarListaVaciaYRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.userRepository.fetchRemoteUsers() } returns emptyList()
        coEvery { deps.userRepository.replaceAllUsers(any()) } returns Unit

        val result = deps.service.syncUsers()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.userRepository.fetchRemoteUsers()
        }

        coVerify(exactly = 1) {
            deps.userRepository.replaceAllUsers(emptyList())
        }
    }

    @Test
    fun syncReglas_configValida_sinReglas_debeGuardarListaVaciaYRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.ruleRepository.fetchRemoteReglas() } returns emptyList()
        coEvery { deps.ruleRepository.replaceAllReglas(any()) } returns Unit

        val result = deps.service.syncReglas()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.ruleRepository.fetchRemoteReglas()
        }

        coVerify(exactly = 1) {
            deps.ruleRepository.replaceAllReglas(emptyList())
        }
    }

    @Test
    fun syncUbicaciones_configValida_sinUbicaciones_debeGuardarListaVaciaYRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.locationRepository.fetchRemoteUbicaciones() } returns emptyList()
        coEvery { deps.locationRepository.replaceAllUbicaciones(any()) } returns Unit

        val result = deps.service.syncUbicaciones()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.locationRepository.fetchRemoteUbicaciones()
        }

        coVerify(exactly = 1) {
            deps.locationRepository.replaceAllUbicaciones(emptyList())
        }
    }

    @Test
    fun syncProductos_configValida_sinProductos_debeGuardarListaVaciaYRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.productRepository.fetchRemoteProductos() } returns emptyList()
        coEvery { deps.productRepository.replaceAllProductos(any()) } returns Unit

        val result = deps.service.syncProductos()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.productRepository.fetchRemoteProductos()
        }

        coVerify(exactly = 1) {
            deps.productRepository.replaceAllProductos(emptyList())
        }
    }

    @Test
    fun syncUnidadMedidas_configValida_sinUnidades_debeGuardarListaVaciaYRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.unitMeasureRepository.fetchRemoteUnidadMedidas() } returns emptyList()
        coEvery { deps.unitMeasureRepository.replaceAllUnidadMedidas(any()) } returns Unit

        val result = deps.service.syncUnidadMedidas()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.unitMeasureRepository.fetchRemoteUnidadMedidas()
        }

        coVerify(exactly = 1) {
            deps.unitMeasureRepository.replaceAllUnidadMedidas(emptyList())
        }
    }

    @Test
    fun syncInventariosRemotos_configValida_sinInventarios_debeGuardarListaVaciaYRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.inventoryRemoteRepository.fetchRemoteInventarios() } returns emptyList()
        coEvery {
            deps.inventoryRemoteRepository.replaceAllInventarios(
                inventarios = any(),
                usuarios = any()
            )
        } returns Unit

        val result = deps.service.syncInventariosRemotos()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.inventoryRemoteRepository.fetchRemoteInventarios()
        }

        coVerify(exactly = 1) {
            deps.inventoryRemoteRepository.replaceAllInventarios(
                inventarios = emptyList(),
                usuarios = emptyList()
            )
        }
    }

    @Test
    fun syncAllCatalogs_configValida_sinDatos_debeRetornarResumenEnCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.ruleRepository.fetchRemoteReglas() } returns emptyList()
        coEvery { deps.ruleRepository.replaceAllReglas(any()) } returns Unit

        coEvery { deps.userRepository.fetchRemoteUsers() } returns emptyList()
        coEvery { deps.userRepository.replaceAllUsers(any()) } returns Unit

        coEvery { deps.locationRepository.fetchRemoteUbicaciones() } returns emptyList()
        coEvery { deps.locationRepository.replaceAllUbicaciones(any()) } returns Unit

        coEvery { deps.productRepository.fetchRemoteProductos() } returns emptyList()
        coEvery { deps.productRepository.replaceAllProductos(any()) } returns Unit

        coEvery { deps.unitMeasureRepository.fetchRemoteUnidadMedidas() } returns emptyList()
        coEvery { deps.unitMeasureRepository.replaceAllUnidadMedidas(any()) } returns Unit

        coEvery { deps.inventoryRemoteRepository.fetchRemoteInventarios() } returns emptyList()
        coEvery {
            deps.inventoryRemoteRepository.replaceAllInventarios(
                inventarios = any(),
                usuarios = any()
            )
        } returns Unit

        coEvery { deps.versionService.checkVersion() } returns null

        val result = deps.service.syncAllCatalogs()

        assertEquals(0, result.users)
        assertEquals(0, result.reglas)
        assertEquals(0, result.ubicaciones)
        assertEquals(0, result.productos)
        assertEquals(0, result.unidadMedidas)
        assertEquals(0, result.inventarios)
        assertEquals(null, result.version)
    }

    @Test
    fun syncAllInventarioPendiente_sinPendientes_debeRetornarResumenEnCero() = runTest {
        val deps = createDepsConfigured()

        coEvery {
            deps.inventoryRepository.getCapturasPendientesSincronizar()
        } returns emptyList()

        coEvery {
            deps.inventoryRepository.getInventariosPendientesFinishSync()
        } returns emptyList()

        val result = deps.service.syncAllInventarioPendiente()

        assertEquals(0, result.capturas)
        assertEquals(0, result.finalizados)

        coVerify(exactly = 1) {
            deps.inventoryRepository.getCapturasPendientesSincronizar()
        }

        coVerify(exactly = 1) {
            deps.inventoryRepository.getInventariosPendientesFinishSync()
        }
    }

    private fun createDepsConfigured(): SyncServiceTestDeps {
        return createDeps(
            empresaRut = "76001910-0",
            apiKey = "api-key-prueba",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = true
        )
    }

    private fun createDeps(
        empresaRut: String = "76001910-0",
        apiKey: String = "api-key-prueba",
        authToken: String = "token-prueba",
        deviceSession: String = "SESSION-001",
        deviceActivated: Boolean = true
    ): SyncServiceTestDeps {
        val userRepository = mockk<UserRepository>(relaxed = true)
        val ruleRepository = mockk<RuleRepository>(relaxed = true)
        val locationRepository = mockk<LocationRepository>(relaxed = true)
        val productRepository = mockk<ProductRepository>(relaxed = true)
        val unitMeasureRepository = mockk<UnitMeasureRepository>(relaxed = true)
        val inventoryRemoteRepository = mockk<InventoryRemoteRepository>(relaxed = true)
        val versionService = mockk<VersionService>(relaxed = true)
        val api = mockk<ApiService>(relaxed = true)
        val settings = mockSettingsManager(
            empresaRut = empresaRut,
            apiKey = apiKey,
            authToken = authToken,
            deviceSession = deviceSession,
            deviceActivated = deviceActivated
        )
        val inventoryRepository = mockk<InventoryRepository>(relaxed = true)
        val syncLogRepository = mockk<SyncLogRepository>(relaxed = true)
        val headersBuilder = mockk<ProtectedHeadersBuilder>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        val service = SyncService(
            userRepository = userRepository,
            ruleRepository = ruleRepository,
            locationRepository = locationRepository,
            productRepository = productRepository,
            unitMeasureRepository = unitMeasureRepository,
            inventoryRemoteRepository = inventoryRemoteRepository,
            versionService = versionService,
            api = api,
            settings = settings,
            inventoryRepository = inventoryRepository,
            syncLogRepository = syncLogRepository,
            headersBuilder = headersBuilder,
            context = context
        )

        return SyncServiceTestDeps(
            service = service,
            userRepository = userRepository,
            ruleRepository = ruleRepository,
            locationRepository = locationRepository,
            productRepository = productRepository,
            unitMeasureRepository = unitMeasureRepository,
            inventoryRemoteRepository = inventoryRemoteRepository,
            versionService = versionService,
            api = api,
            settings = settings,
            inventoryRepository = inventoryRepository,
            syncLogRepository = syncLogRepository,
            headersBuilder = headersBuilder,
            context = context
        )
    }

    private fun mockSettingsManager(
        empresaRut: String,
        apiKey: String,
        authToken: String,
        deviceSession: String,
        deviceActivated: Boolean
    ): SettingsManager {
        val settings = mockk<SettingsManager>(relaxed = true)

        every { settings.empresaRut } returns MutableStateFlow(empresaRut)
        every { settings.apiKey } returns MutableStateFlow(apiKey)
        every { settings.authToken } returns MutableStateFlow(authToken)
        every { settings.deviceSession } returns MutableStateFlow(deviceSession)
        every { settings.deviceActivated } returns MutableStateFlow(deviceActivated)

        return settings
    }

    private data class SyncServiceTestDeps(
        val service: SyncService,
        val userRepository: UserRepository,
        val ruleRepository: RuleRepository,
        val locationRepository: LocationRepository,
        val productRepository: ProductRepository,
        val unitMeasureRepository: UnitMeasureRepository,
        val inventoryRemoteRepository: InventoryRemoteRepository,
        val versionService: VersionService,
        val api: ApiService,
        val settings: SettingsManager,
        val inventoryRepository: InventoryRepository,
        val syncLogRepository: SyncLogRepository,
        val headersBuilder: ProtectedHeadersBuilder,
        val context: Context
    )
}