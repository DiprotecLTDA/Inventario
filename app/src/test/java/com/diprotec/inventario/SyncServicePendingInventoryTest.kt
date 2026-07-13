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

class SyncServicePendingInventoryTest {

    /*
     * Pruebas unitarias para la sincronización de inventarios pendientes.
     *
     * Este test valida el comportamiento base de:
     *
     * 1. syncRegistroInventarios().
     * 2. syncFinishInventarios().
     * 3. syncAllInventarioPendiente().
     * 4. finishInventarioRemoto().
     *
     * Validaciones cubiertas:
     * 1. Si no existen capturas pendientes, syncRegistroInventarios retorna 0.
     * 2. Si no existen inventarios finalizados pendientes, syncFinishInventarios retorna 0.
     * 3. Si falla la consulta de capturas pendientes, syncRegistroInventarios propaga el error.
     * 4. Si falla la consulta de inventarios finalizados pendientes, syncFinishInventarios propaga el error.
     * 5. Si syncRegistroInventarios falla dentro de syncAllInventarioPendiente(),
     *    el resumen retorna capturas = 0 y continúa con finalizados.
     * 6. Si syncFinishInventarios falla dentro de syncAllInventarioPendiente(),
     *    el resumen retorna finalizados = 0 y conserva el resultado de capturas.
     * 7. Si no hay pendientes de ningún tipo, syncAllInventarioPendiente retorna resumen en cero.
     * 8. Si un inventario finalizado tiene remoteInventoryId inválido, se ignora y retorna 0.
     * 9. Si finishInventarioRemoto no encuentra inventario local, retorna error.
     * 10. Si finishInventarioRemoto encuentra inventario con remoteInventoryId inválido,
     *     retorna error antes de llamar a la API.
     *
     * Estas pruebas no validan todavía el envío exitoso de capturas ni el body
     * enviado a la API, porque para eso se requieren los modelos concretos de
     * capturas/inventarios y el DTO real de respuesta de ApiService.
     */

    @Test
    fun syncRegistroInventarios_sinCapturasPendientes_debeRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery {
            deps.inventoryRepository.getCapturasPendientesSincronizar()
        } returns emptyList()

        val result = deps.service.syncRegistroInventarios()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.inventoryRepository.getCapturasPendientesSincronizar()
        }

        coVerify(exactly = 0) {
            deps.api.sendRegistroInventario(
                empresaRUT = any(),
                apiKey = any(),
                authorization = any(),
                deviceSession = any(),
                deviceSignature = any(),
                deviceTimestamp = any(),
                body = any()
            )
        }
    }

    @Test
    fun syncFinishInventarios_sinFinalizadosPendientes_debeRetornarCero() = runTest {
        val deps = createDepsConfigured()

        coEvery {
            deps.inventoryRepository.getInventariosPendientesFinishSync()
        } returns emptyList()

        val result = deps.service.syncFinishInventarios()

        assertEquals(0, result)

        coVerify(exactly = 1) {
            deps.inventoryRepository.getInventariosPendientesFinishSync()
        }

        coVerify(exactly = 0) {
            deps.api.finishInventario(
                empresaRUT = any(),
                apiKey = any(),
                authorization = any(),
                deviceSession = any(),
                deviceSignature = any(),
                deviceTimestamp = any(),
                body = any()
            )
        }
    }

    @Test
    fun syncRegistroInventarios_errorConsultandoCapturas_debePropagarError() = runTest {
        val deps = createDepsConfigured()

        coEvery {
            deps.inventoryRepository.getCapturasPendientesSincronizar()
        } throws IOException("sin conexión")

        val error = runCatching {
            deps.service.syncRegistroInventarios()
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals("sin conexión", error?.message)

        coVerify(exactly = 0) {
            deps.api.sendRegistroInventario(
                empresaRUT = any(),
                apiKey = any(),
                authorization = any(),
                deviceSession = any(),
                deviceSignature = any(),
                deviceTimestamp = any(),
                body = any()
            )
        }
    }

    @Test
    fun syncFinishInventarios_errorConsultandoFinalizados_debePropagarError() = runTest {
        val deps = createDepsConfigured()

        coEvery {
            deps.inventoryRepository.getInventariosPendientesFinishSync()
        } throws IOException("sin conexión")

        val error = runCatching {
            deps.service.syncFinishInventarios()
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals("sin conexión", error?.message)

        coVerify(exactly = 0) {
            deps.api.finishInventario(
                empresaRUT = any(),
                apiKey = any(),
                authorization = any(),
                deviceSession = any(),
                deviceSignature = any(),
                deviceTimestamp = any(),
                body = any()
            )
        }
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

    @Test
    fun syncAllInventarioPendiente_siCapturasFallaYFinalizadosVacio_debeRetornarResumenEnCero() =
        runTest {
            val deps = createDepsConfigured()

            coEvery {
                deps.inventoryRepository.getCapturasPendientesSincronizar()
            } throws IOException("error capturas")

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

    @Test
    fun syncAllInventarioPendiente_siCapturasVacioYFinalizadosFalla_debeRetornarResumenEnCero() =
        runTest {
            val deps = createDepsConfigured()

            coEvery {
                deps.inventoryRepository.getCapturasPendientesSincronizar()
            } returns emptyList()

            coEvery {
                deps.inventoryRepository.getInventariosPendientesFinishSync()
            } throws IOException("error finalizados")

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

    @Test
    fun syncFinishInventarios_conRemoteInventoryIdInvalido_debeIgnorarYRetornarCero() =
        runTest {
            val deps = createDepsConfigured()

            /*
             * Se usa un mock relajado del tipo real que retorna
             * getInventariosPendientesFinishSync().
             *
             * Al ser relaxed, remoteInventoryId queda como String vacío,
             * por lo tanto toLongOrNull() retorna null y el servicio debe ignorarlo.
             */
            coEvery {
                deps.inventoryRepository.getInventariosPendientesFinishSync()
            } returns listOf(mockk(relaxed = true))

            val result = deps.service.syncFinishInventarios()

            assertEquals(0, result)

            coVerify(exactly = 0) {
                deps.api.finishInventario(
                    empresaRUT = any(),
                    apiKey = any(),
                    authorization = any(),
                    deviceSession = any(),
                    deviceSignature = any(),
                    deviceTimestamp = any(),
                    body = any()
                )
            }
        }

    @Test
    fun finishInventarioRemoto_inventarioLocalNoExiste_debeRetornarError() = runTest {
        val deps = createDepsConfigured()

        coEvery {
            deps.inventoryRepository.getInventoryById(10L)
        } returns null

        val error = runCatching {
            deps.service.finishInventarioRemoto(
                inventoryId = 10L,
                usuarioRut = "13056459-3"
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("No se encontró el inventario local", error?.message)

        coVerify(exactly = 0) {
            deps.api.finishInventario(
                empresaRUT = any(),
                apiKey = any(),
                authorization = any(),
                deviceSession = any(),
                deviceSignature = any(),
                deviceTimestamp = any(),
                body = any()
            )
        }
    }

    @Test
    fun finishInventarioRemoto_remoteInventoryIdInvalido_debeRetornarErrorSinLlamarApi() =
        runTest {
            val deps = createDepsConfigured()

            /*
             * Se usa un mock relajado del tipo real de inventario local.
             * remoteInventoryId queda como String vacío y falla al convertirlo a Long.
             */
            coEvery {
                deps.inventoryRepository.getInventoryById(10L)
            } returns mockk(relaxed = true)

            val error = runCatching {
                deps.service.finishInventarioRemoto(
                    inventoryId = 10L,
                    usuarioRut = "13056459-3"
                )
            }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("remoteInventoryId inválido: ", error?.message)

            coVerify(exactly = 0) {
                deps.api.finishInventario(
                    empresaRUT = any(),
                    apiKey = any(),
                    authorization = any(),
                    deviceSession = any(),
                    deviceSignature = any(),
                    deviceTimestamp = any(),
                    body = any()
                )
            }
        }

    private fun createDepsConfigured(): SyncServicePendingInventoryTestDeps {
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
    ): SyncServicePendingInventoryTestDeps {
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

        return SyncServicePendingInventoryTestDeps(
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

    private data class SyncServicePendingInventoryTestDeps(
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