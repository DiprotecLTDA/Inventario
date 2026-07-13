package com.diprotec.inventario.ui.inventory.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptureInventoryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /*
     * Pruebas instrumentadas para CaptureInventoryScreen.
     *
     * Este test valida el comportamiento visual básico del módulo
     * Toma de Inventario / Continuar Inventario.
     *
     * Validaciones cubiertas:
     * 1. La pantalla muestra los controles base de captura.
     * 2. En modo Unidad no se muestra el campo Cantidad.
     * 3. En modo Unidad no se muestra el botón Registrar.
     * 4. Al seleccionar modo Cantidad se muestra el campo Cantidad.
     * 5. Al seleccionar modo Cantidad se muestra el botón Registrar.
     * 6. Al volver a modo Unidad se oculta el campo Cantidad.
     * 7. Al volver a modo Unidad se oculta el botón Registrar.
     * 8. El botón Ver ejecuta su callback.
     * 9. El botón Pendiente ejecuta su callback.
     * 10. El botón Finalizar existe en pantalla.
     *
     * En estas pruebas se usa enableScanner = false para evitar que el servicio
     * físico del lector Unitech interfiera con la Activity de pruebas.
     */

    @Test
    fun pantallaInicial_modoUnidad_debeMostrarControlesBase() {
        val state = MutableStateFlow(
            CaptureInventoryUiState(
                scanMode = CaptureMode.UNIT,
                selectedUbicacionName = "",
                barcode = "",
                selectedUnitName = "",
                description = ""
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel = viewModel)

        assertTagExists("capture_screen")
        assertTagExists("input_scanner_buffer")
        assertTagExists("input_ubicacion")
        assertTagExists("selector_capture_mode")
        assertTagExists("capture_mode_unit")
        assertTagExists("capture_mode_quantity")
        assertTagExists("input_barcode")
        assertTagExists("input_unit_measure")
        assertTagExists("input_description")
        assertTagExists("btn_view_inventory")
        assertTagExists("btn_leave_pending")
        assertTagExists("btn_finish_inventory")

        assertTagDoesNotExist("input_quantity")
        assertTagDoesNotExist("btn_register_capture")
    }

    @Test
    fun seleccionarCantidad_debeMostrarCampoCantidadYBotonRegistrar() {
        val state = MutableStateFlow(
            CaptureInventoryUiState(
                scanMode = CaptureMode.UNIT,
                selectedUbicacionName = "",
                barcode = "",
                selectedUnitName = "",
                description = ""
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel = viewModel)

        composeRule.onNodeWithTag("capture_mode_quantity").performClick()
        composeRule.waitForIdle()

        assertTagExists("input_quantity")
        assertTagExists("btn_register_capture")

        verify(exactly = 1) {
            viewModel.onScanModeChanged(CaptureMode.QUANTITY)
        }
    }

    @Test
    fun seleccionarUnidad_debeOcultarCampoCantidadYBotonRegistrar() {
        val state = MutableStateFlow(
            CaptureInventoryUiState(
                scanMode = CaptureMode.QUANTITY,
                selectedUbicacionName = "",
                barcode = "",
                quantityInput = "1",
                selectedUnitName = "",
                description = ""
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel = viewModel)

        assertTagExists("input_quantity")
        assertTagExists("btn_register_capture")

        composeRule.onNodeWithTag("capture_mode_unit").performClick()
        composeRule.waitForIdle()

        assertTagDoesNotExist("input_quantity")
        assertTagDoesNotExist("btn_register_capture")

        verify(exactly = 1) {
            viewModel.onScanModeChanged(CaptureMode.UNIT)
        }
    }

    @Test
    fun botonVer_debeEjecutarCallback() {
        val state = MutableStateFlow(
            CaptureInventoryUiState(
                scanMode = CaptureMode.UNIT
            )
        )

        val viewModel = mockViewModel(state)
        var viewClicked = false

        setScreenContent(
            viewModel = viewModel,
            onViewList = { viewClicked = true }
        )

        composeRule.onNodeWithTag("btn_view_inventory").performClick()
        composeRule.waitForIdle()

        assertTrue(viewClicked)
    }

    @Test
    fun botonPendiente_debeEjecutarCallback() {
        val state = MutableStateFlow(
            CaptureInventoryUiState(
                scanMode = CaptureMode.UNIT
            )
        )

        val viewModel = mockViewModel(state)
        var pendingClicked = false

        setScreenContent(
            viewModel = viewModel,
            onLeavePending = { pendingClicked = true }
        )

        composeRule.onNodeWithTag("btn_leave_pending").performClick()
        composeRule.waitForIdle()

        assertTrue(pendingClicked)
    }

    @Test
    fun botonFinalizar_debeExistirEnPantalla() {
        val state = MutableStateFlow(
            CaptureInventoryUiState(
                scanMode = CaptureMode.UNIT
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(
            viewModel = viewModel,
            onFinishInventory = {}
        )

        assertTagExists("btn_finish_inventory")
    }

    private fun setScreenContent(
        viewModel: CaptureInventoryViewModel,
        onViewList: () -> Unit = {},
        onLeavePending: () -> Unit = {},
        onFinishInventory: () -> Unit = {}
    ) {
        composeRule.setContent {
            CaptureInventoryScreen(
                inventoryId = 100L,
                onBack = {},
                onViewList = onViewList,
                onLeavePending = onLeavePending,
                onFinishInventory = onFinishInventory,
                viewModel = viewModel,
                enableScanner = false
            )
        }

        composeRule.waitForIdle()
    }

    private fun mockViewModel(
        state: MutableStateFlow<CaptureInventoryUiState>
    ): CaptureInventoryViewModel {
        val viewModel = mockk<CaptureInventoryViewModel>(relaxed = true)

        every { viewModel.uiState } returns state

        every { viewModel.loadInventory(any()) } just Runs

        every {
            viewModel.onScanModeChanged(CaptureMode.QUANTITY)
        } answers {
            state.value = state.value.copy(
                scanMode = CaptureMode.QUANTITY,
                quantityInput = state.value.quantityInput.ifBlank { "1" }
            )
            Unit
        }

        every {
            viewModel.onScanModeChanged(CaptureMode.UNIT)
        } answers {
            state.value = state.value.copy(
                scanMode = CaptureMode.UNIT,
                quantityInput = ""
            )
            Unit
        }

        every { viewModel.onQuantityChanged(any()) } just Runs
        every { viewModel.onUnitSelected(any()) } just Runs
        every { viewModel.onUbicacionSelected(any()) } just Runs
        every { viewModel.onBarcodeDetected(any()) } just Runs
        every { viewModel.registerDetectedBarcode(any(), any()) } just Runs
        every { viewModel.registerCurrentScan(any()) } just Runs
        every { viewModel.finalizeInventory(any(), any()) } just Runs

        return viewModel
    }

    private fun assertTagExists(tag: String) {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
    }

    private fun assertTagDoesNotExist(tag: String) {
        val nodes = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertEquals(0, nodes.size)
    }
}