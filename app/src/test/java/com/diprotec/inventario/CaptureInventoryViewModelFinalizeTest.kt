package com.diprotec.inventario.ui.inventory.capture

import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import com.diprotec.inventario.data.repository.InventoryRepository
import com.diprotec.inventario.data.repository.UnitMeasureRepository
import com.diprotec.inventario.service.SyncService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class CaptureInventoryViewModelFinalizeTest {

    private lateinit var repository: InventoryRepository
    private lateinit var unitMeasureRepository: UnitMeasureRepository
    private lateinit var session: SessionManager
    private lateinit var syncService: SyncService

    /*
     * Pruebas unitarias para la finalización de inventario.
     *
     * Validaciones cubiertas:
     * 1. Si no hay usuario logueado, no finaliza el inventario.
     * 2. Si no hay usuario logueado, muestra error "No hay usuario logueado".
     * 3. Si no hay usuario logueado, no ejecuta onFinished.
     * 4. Si el inventario está pendiente, finaliza localmente llamando a repository.finalizeInventory(id).
     * 5. Al finalizar correctamente, actualiza inventoryStatus a FINISHED.
     * 6. Al finalizar correctamente, muestra mensaje de éxito local.
     * 7. Al finalizar correctamente, sincroniza capturas pendientes.
     * 8. Al finalizar correctamente, intenta finalizar el inventario remoto.
     * 9. Al finalizar correctamente, ejecuta onFinished.
     * 10. Si el inventario ya estaba FINISHED, ejecuta onFinished sin volver a finalizar localmente.
     * 11. Si falla la sincronización remota, mantiene el inventario finalizado localmente.
     * 12. Si falla la sincronización remota, muestra mensaje de cierre pendiente de sincronizar.
     * 13. Si falla la sincronización remota, igualmente ejecuta onFinished.
     *
     * Estas pruebas no usan base de datos real ni API real.
     * Se simulan Repository, SessionManager, UnitMeasureRepository y SyncService con MockK.
     */

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        unitMeasureRepository = mockk(relaxed = true)
        session = mockk(relaxed = true)
        syncService = mockk(relaxed = true)

        every { unitMeasureRepository.observeUnidadMedidas() } returns emptyFlow()
        every { repository.observeUbicacionesActivas() } returns emptyFlow()

        coEvery { repository.finalizeInventory(any()) } returns Unit

        coEvery {
            syncService.syncRegistroInventarios()
        } returns 1

        coEvery {
            syncService.finishInventarioRemoto(
                inventoryId = any(),
                usuarioRut = any()
            )
        } returns true
    }

    @Test
    fun finalizarInventario_sinUsuarioLogueado_debeMostrarErrorYNoFinalizar() {
        every { session.loginRut } returns MutableStateFlow("")

        val viewModel = createViewModel()
        var finishedCalled = false

        viewModel.finalizeInventory(
            inventoryId = 100L,
            onFinished = { finishedCalled = true }
        )

        waitUntil {
            viewModel.uiState.value.errorMessage == "No hay usuario logueado"
        }

        val state = viewModel.uiState.value

        assertEquals("No hay usuario logueado", state.errorMessage)
        assertNull(state.successMessage)
        assertFalse(finishedCalled)

        coVerify(exactly = 0) {
            repository.finalizeInventory(any())
        }

        coVerify(exactly = 0) {
            syncService.syncRegistroInventarios()
        }

        coVerify(exactly = 0) {
            syncService.finishInventarioRemoto(
                inventoryId = any(),
                usuarioRut = any()
            )
        }
    }

    @Test
    fun finalizarInventario_pendiente_debeFinalizarLocalSincronizarYEjecutarCallback() {
        every { session.loginRut } returns MutableStateFlow("13056459-3")

        val viewModel = createViewModel()
        var finishedCalled = false

        viewModel.finalizeInventory(
            inventoryId = 100L,
            onFinished = { finishedCalled = true }
        )

        waitUntil {
            viewModel.uiState.value.inventoryStatus == InventoryStatus.FINISHED.name &&
                    finishedCalled
        }

        val state = viewModel.uiState.value

        assertEquals(InventoryStatus.FINISHED.name, state.inventoryStatus)
        assertEquals("Inventario finalizado localmente", state.successMessage)
        assertNull(state.errorMessage)
        assertTrue(finishedCalled)

        coVerify(exactly = 1) {
            repository.finalizeInventory(100L)
        }

        coVerify(exactly = 1) {
            syncService.syncRegistroInventarios()
        }

        coVerify(exactly = 1) {
            syncService.finishInventarioRemoto(
                inventoryId = 100L,
                usuarioRut = "13056459-3"
            )
        }
    }

    @Test
    fun finalizarInventario_yaFinalizado_debeEjecutarCallbackSinVolverAFinalizar() {
        every { session.loginRut } returns MutableStateFlow("13056459-3")

        val viewModel = createViewModel()
        var firstFinishedCalled = false
        var secondFinishedCalled = false

        viewModel.finalizeInventory(
            inventoryId = 100L,
            onFinished = { firstFinishedCalled = true }
        )

        waitUntil {
            viewModel.uiState.value.inventoryStatus == InventoryStatus.FINISHED.name &&
                    firstFinishedCalled
        }

        viewModel.finalizeInventory(
            inventoryId = 100L,
            onFinished = { secondFinishedCalled = true }
        )

        waitUntil {
            secondFinishedCalled
        }

        assertTrue(firstFinishedCalled)
        assertTrue(secondFinishedCalled)
        assertEquals(
            InventoryStatus.FINISHED.name,
            viewModel.uiState.value.inventoryStatus
        )

        coVerify(exactly = 1) {
            repository.finalizeInventory(100L)
        }

        coVerify(exactly = 1) {
            syncService.syncRegistroInventarios()
        }

        coVerify(exactly = 1) {
            syncService.finishInventarioRemoto(
                inventoryId = 100L,
                usuarioRut = "13056459-3"
            )
        }
    }

    @Test
    fun finalizarInventario_siFallaSyncRemoto_debeQuedarFinalizadoLocalYPendienteSincronizar() {
        every { session.loginRut } returns MutableStateFlow("13056459-3")

        coEvery {
            syncService.syncRegistroInventarios()
        } returns 1

        coEvery {
            syncService.finishInventarioRemoto(
                inventoryId = any(),
                usuarioRut = any()
            )
        } throws RuntimeException("Error remoto")

        val viewModel = createViewModel()
        var finishedCalled = false

        viewModel.finalizeInventory(
            inventoryId = 100L,
            onFinished = { finishedCalled = true }
        )

        waitUntil {
            viewModel.uiState.value.inventoryStatus == InventoryStatus.FINISHED.name &&
                    viewModel.uiState.value.errorMessage != null &&
                    finishedCalled
        }

        val state = viewModel.uiState.value

        assertEquals(InventoryStatus.FINISHED.name, state.inventoryStatus)
        assertEquals(
            "Finalizado localmente. Pendiente de sincronizar cierre: Error remoto",
            state.errorMessage
        )
        assertNull(state.successMessage)
        assertTrue(finishedCalled)

        coVerify(exactly = 1) {
            repository.finalizeInventory(100L)
        }

        coVerify(exactly = 1) {
            syncService.syncRegistroInventarios()
        }

        coVerify(exactly = 1) {
            syncService.finishInventarioRemoto(
                inventoryId = 100L,
                usuarioRut = "13056459-3"
            )
        }
    }

    private fun createViewModel(): CaptureInventoryViewModel {
        return CaptureInventoryViewModel(
            repository = repository,
            unitMeasureRepository = unitMeasureRepository,
            session = session,
            syncService = syncService
        )
    }

    private fun waitUntil(
        timeoutMillis: Long = 5_000L,
        condition: () -> Boolean
    ) {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMillis) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            if (condition()) {
                return
            }

            Thread.sleep(25)
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(
            "No se cumplió la condición esperada dentro de $timeoutMillis ms",
            condition()
        )
    }
}