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
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncServiceCatalogFailureTest {

    /*
     * Pruebas unitarias para fallos parciales en SyncService.syncAllCatalogs().
     *
     * Este test valida que la sincronización general de catálogos sea tolerante
     * a errores individuales.
     *
     * Validaciones cubiertas:
     * 1. Si falla syncUsers(), syncAllCatalogs() no se cae y retorna users = 0.
     * 2. Si falla syncReglas(), syncAllCatalogs() no se cae y retorna reglas = 0.
     * 3. Si falla syncProductos(), syncAllCatalogs() no se cae y retorna productos = 0.
     * 4. Si falla syncVersion(), syncAllCatalogs() no se cae y retorna version = null.
     * 5. Si falta configuración, syncAllCatalogs() retorna error antes de consultar repositorios.
     * 6. Si el dispositivo no está activado, syncAllCatalogs() retorna error antes de consultar repositorios.
     *
     * Estas pruebas no usan datos reales de catálogos. Se enfocan en validar
     * continuidad del flujo, manejo de errores y protección por configuración.
     */

    @Test
    fun syncAllCatalogs_siUsuariosFalla_debeContinuarYRetornarUsersCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.ruleRepository.fetchRemoteReglas() } returns emptyList()
        coEvery { deps.ruleRepository.replaceAllReglas(any()) } returns Unit

        coEvery { deps.userRepository.fetchRemoteUsers() } throws IOException("error usuarios")

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

        coVerify(exactly = 1) { deps.userRepository.fetchRemoteUsers() }
        coVerify(exactly = 0) { deps.userRepository.replaceAllUsers(any()) }

        coVerify(exactly = 1) { deps.ruleRepository.fetchRemoteReglas() }
        coVerify(exactly = 1) { deps.locationRepository.fetchRemoteUbicaciones() }
        coVerify(exactly = 1) { deps.productRepository.fetchRemoteProductos() }
        coVerify(exactly = 1) { deps.unitMeasureRepository.fetchRemoteUnidadMedidas() }
        coVerify(exactly = 1) { deps.inventoryRemoteRepository.fetchRemoteInventarios() }
        coVerify(exactly = 1) { deps.versionService.checkVersion() }
    }

    @Test
    fun syncAllCatalogs_siReglasFalla_debeContinuarYRetornarReglasCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.ruleRepository.fetchRemoteReglas() } throws IOException("error reglas")

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

        assertEquals(0, result.reglas)
        assertEquals(0, result.users)
        assertEquals(0, result.ubicaciones)
        assertEquals(0, result.productos)
        assertEquals(0, result.unidadMedidas)
        assertEquals(0, result.inventarios)
        assertEquals(null, result.version)

        coVerify(exactly = 1) { deps.ruleRepository.fetchRemoteReglas() }
        coVerify(exactly = 0) { deps.ruleRepository.replaceAllReglas(any()) }

        coVerify(exactly = 1) { deps.userRepository.fetchRemoteUsers() }
        coVerify(exactly = 1) { deps.locationRepository.fetchRemoteUbicaciones() }
        coVerify(exactly = 1) { deps.productRepository.fetchRemoteProductos() }
        coVerify(exactly = 1) { deps.unitMeasureRepository.fetchRemoteUnidadMedidas() }
        coVerify(exactly = 1) { deps.inventoryRemoteRepository.fetchRemoteInventarios() }
        coVerify(exactly = 1) { deps.versionService.checkVersion() }
    }

    @Test
    fun syncAllCatalogs_siProductosFalla_debeContinuarYRetornarProductosCero() = runTest {
        val deps = createDepsConfigured()

        coEvery { deps.ruleRepository.fetchRemoteReglas() } returns emptyList()
        coEvery { deps.ruleRepository.replaceAllReglas(any()) } returns Unit

        coEvery { deps.userRepository.fetchRemoteUsers() } returns emptyList()
        coEvery { deps.userRepository.replaceAllUsers(any()) } returns Unit

        coEvery { deps.locationRepository.fetchRemoteUbicaciones() } returns emptyList()
        coEvery { deps.locationRepository.replaceAllUbicaciones(any()) } returns Unit

        coEvery { deps.productRepository.fetchRemoteProductos() } throws IOException("error productos")

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

        assertEquals(0, result.productos)
        assertEquals(0, result.users)
        assertEquals(0, result.reglas)
        assertEquals(0, result.ubicaciones)
        assertEquals(0, result.unidadMedidas)
        assertEquals(0, result.inventarios)
        assertEquals(null, result.version)

        coVerify(exactly = 1) { deps.productRepository.fetchRemoteProductos() }
        coVerify(exactly = 0) { deps.productRepository.replaceAllProductos(any()) }

        coVerify(exactly = 1) { deps.ruleRepository.fetchRemoteReglas() }
        coVerify(exactly = 1) { deps.userRepository.fetchRemoteUsers() }
        coVerify(exactly = 1) { deps.locationRepository.fetchRemoteUbicaciones() }
        coVerify(exactly = 1) { deps.unitMeasureRepository.fetchRemoteUnidadMedidas() }
        coVerify(exactly = 1) { deps.inventoryRemoteRepository.fetchRemoteInventarios() }
        coVerify(exactly = 1) { deps.versionService.checkVersion() }
    }

    @Test
    fun syncAllCatalogs_siVersionFalla_debeContinuarYRetornarVersionNull() = runTest {
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

        coEvery { deps.versionService.checkVersion() } throws IOException("error version")

        val result = deps.service.syncAllCatalogs()

        assertEquals(0, result.users)
        assertEquals(0, result.reglas)
        assertEquals(0, result.ubicaciones)
        assertEquals(0, result.productos)
        assertEquals(0, result.unidadMedidas)
        assertEquals(0, result.inventarios)
        assertEquals(null, result.version)

        coVerify(exactly = 1) { deps.versionService.checkVersion() }
    }

    @Test
    fun syncAllCatalogs_sinEmpresaRut_debeRetornarErrorYNoConsultarRepositorios() = runTest {
        val deps = createDeps(
            empresaRut = "",
            apiKey = "api-key-prueba",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = true
        )

        val error = runCatching {
            deps.service.syncAllCatalogs()
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Empresa RUT no configurado", error?.message)

        verifyNoCatalogRepositoryCalls(deps)
    }

    @Test
    fun syncAllCatalogs_dispositivoNoActivado_debeRetornarErrorYNoConsultarRepositorios() = runTest {
        val deps = createDeps(
            empresaRut = "76001910-0",
            apiKey = "api-key-prueba",
            authToken = "token-prueba",
            deviceSession = "SESSION-001",
            deviceActivated = false
        )

        val error = runCatching {
            deps.service.syncAllCatalogs()
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("El dispositivo no está activado", error?.message)

        verifyNoCatalogRepositoryCalls(deps)
    }

    private fun verifyNoCatalogRepositoryCalls(deps: SyncServiceCatalogFailureTestDeps) {
        coVerify(exactly = 0) { deps.ruleRepository.fetchRemoteReglas() }
        coVerify(exactly = 0) { deps.userRepository.fetchRemoteUsers() }
        coVerify(exactly = 0) { deps.locationRepository.fetchRemoteUbicaciones() }
        coVerify(exactly = 0) { deps.productRepository.fetchRemoteProductos() }
        coVerify(exactly = 0) { deps.unitMeasureRepository.fetchRemoteUnidadMedidas() }
        coVerify(exactly = 0) { deps.inventoryRemoteRepository.fetchRemoteInventarios() }
        coVerify(exactly = 0) { deps.versionService.checkVersion() }
    }

    private fun createDepsConfigured(): SyncServiceCatalogFailureTestDeps {
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
    ): SyncServiceCatalogFailureTestDeps {
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

        return SyncServiceCatalogFailureTestDeps(
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

    private data class SyncServiceCatalogFailureTestDeps(
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