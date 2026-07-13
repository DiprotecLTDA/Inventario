package com.diprotec.inventario.ui.inventory.list

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.diprotec.inventario.data.local.entity.InventoryItemEntity
import com.diprotec.inventario.data.local.inventory.InventoryGroupedRow
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

class InventoryListScreenTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun pantallaDesagrupada_debeMostrarDatosDelInventarioYCapturas() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Bodega Central",
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                )
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        assertTagExists("inventory_list_screen")
        assertTagExists("inventory_name")
        assertTagExists("radio_ungrouped")
        assertTagExists("radio_grouped")
        assertTagExists("inventory_ungrouped_list")
        assertTagExists("inventory_item_10")
        assertTagExists("btn_inventory_list_back")

        assertTextExists("Listado de inventario")
        assertTextExists("Inventario: Inventario Bodega Central")
        assertTextExists("Código: 780000000001")
        assertTextExists("Descripción: Producto prueba uno")
        assertTextExists("Cantidad: 2")
        assertTextExists("Unidad: C/U")
        assertTextExists("Ubicación: Bodega A")

        assertTagDoesNotExist("inventory_grouped_list")
    }

    @Test
    fun presionarAgrupar_debeCambiarAModoAgrupadoYMostrarTotales() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Bodega Central",
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                ),
                groupedItems = emptyList()
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        composeRule.onNodeWithTag("radio_grouped").performClick()
        composeRule.waitForIdle()

        assertTagExists("inventory_grouped_list")
        assertTagExists("inventory_grouped_item_780000000001")

        assertTextExists("Código: 780000000001")
        assertTextExists("Descripción: Producto prueba uno")
        assertTextExists("Cantidad total: 5")
        assertTextExists("Unidad: C/U")
        assertTextExists("Ubicación: Bodega A")
        assertTextExists("Registros agrupados: 3")

        verify(exactly = 1) {
            viewModel.setGrouped(true)
        }
    }

    @Test
    fun presionarDesagrupar_debeCambiarAModoDesagrupado() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = true,
                inventoryName = "Inventario Bodega Central",
                groupedItems = listOf(
                    groupedItem(
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        totalQuantity = 5.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A",
                        totalRows = 3
                    )
                )
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        composeRule.onNodeWithTag("radio_ungrouped").performClick()
        composeRule.waitForIdle()

        assertTagExists("inventory_ungrouped_list")

        verify(exactly = 1) {
            viewModel.setGrouped(false)
        }
    }

    @Test
    fun tocarItemDesagrupado_debeMostrarDialogoEliminar() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Bodega Central",
                canDeleteItems = true,
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                )
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        composeRule.onNodeWithTag("inventory_item_10").performClick()
        composeRule.waitForIdle()

        assertTextExists("Eliminar captura")
        assertTextExists("¿Desea eliminar la captura del producto 780000000001?")
        assertTagExists("btn_confirm_delete_inventory_item")
        assertTagExists("btn_cancel_delete_inventory_item")
    }

    @Test
    fun cancelarEliminacion_debeCerrarDialogoSinEliminar() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Bodega Central",
                canDeleteItems = true,
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                )
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        composeRule.onNodeWithTag("inventory_item_10").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_cancel_delete_inventory_item").performClick()
        composeRule.waitForIdle()

        assertTagDoesNotExist("btn_confirm_delete_inventory_item")

        verify(exactly = 0) {
            viewModel.deleteItem(
                inventoryId = 100L,
                itemId = 10L
            )
        }
    }

    @Test
    fun confirmarEliminacion_debeLlamarDeleteItem() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Bodega Central",
                canDeleteItems = true,
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                )
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        composeRule.onNodeWithTag("inventory_item_10").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_delete_inventory_item").performClick()
        composeRule.waitForIdle()

        verify(exactly = 1) {
            viewModel.deleteItem(
                inventoryId = 100L,
                itemId = 10L
            )
        }
    }

    @Test
    fun tocarItemFinalizado_noDebeMostrarDialogoEliminar() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Finalizado",
                canDeleteItems = false,
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                )
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        composeRule.onNodeWithTag("inventory_item_10").performClick()
        composeRule.waitForIdle()

        assertTagDoesNotExist("btn_confirm_delete_inventory_item")
        assertTextExists("Inventario finalizado: las capturas solo se pueden visualizar.")

        verify(exactly = 0) {
            viewModel.deleteItem(
                inventoryId = 100L,
                itemId = 10L
            )
        }
    }

    @Test
    fun botonVolver_debeEjecutarCallback() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "Inventario Bodega Central"
            )
        )

        val viewModel = mockViewModel(state)
        var backClicked = false

        setScreenContent(
            viewModel = viewModel,
            onBack = { backClicked = true }
        )

        composeRule.onNodeWithTag("btn_inventory_list_back").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(backClicked)
        }
    }

    @Test
    fun cuandoHayError_debeMostrarMensajeError() {
        val state = MutableStateFlow(
            InventoryListUiState(
                isGrouped = false,
                inventoryName = "",
                errorMessage = "No se encontró el inventario"
            )
        )

        val viewModel = mockViewModel(state)

        setScreenContent(viewModel)

        assertTagExists("inventory_error_message")
        assertTextExists("No se encontró el inventario")
    }

    private fun setScreenContent(
        viewModel: InventoryListViewModel,
        onBack: () -> Unit = {}
    ) {
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                InventoryListScreen(
                    inventoryId = 100L,
                    onBack = onBack,
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitForIdle()
    }

    private fun mockViewModel(
        state: MutableStateFlow<InventoryListUiState>
    ): InventoryListViewModel {
        val viewModel = mockk<InventoryListViewModel>(relaxed = true)

        every { viewModel.uiState(100L) } returns state

        every {
            viewModel.setGrouped(true)
        } answers {
            state.value = state.value.copy(
                isGrouped = true,
                groupedItems = listOf(
                    groupedItem(
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        totalQuantity = 5.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A",
                        totalRows = 3
                    )
                )
            )
            Unit
        }

        every {
            viewModel.setGrouped(false)
        } answers {
            state.value = state.value.copy(
                isGrouped = false,
                ungroupedItems = listOf(
                    inventoryItem(
                        id = 10L,
                        barcode = "780000000001",
                        description = "Producto prueba uno",
                        quantity = 2.0,
                        unitMeasure = "C/U",
                        ubicacionNombre = "Bodega A"
                    )
                )
            )
            Unit
        }

        every {
            viewModel.deleteItem(
                inventoryId = any(),
                itemId = any()
            )
        } just Runs

        return viewModel
    }

    private fun inventoryItem(
        id: Long,
        barcode: String,
        description: String,
        quantity: Double,
        unitMeasure: String,
        ubicacionNombre: String
    ): InventoryItemEntity {
        val item = mockk<InventoryItemEntity>(relaxed = true)

        every { item.id } returns id
        every { item.barcode } returns barcode
        every { item.description } returns description
        every { item.quantity } returns quantity
        every { item.unitMeasure } returns unitMeasure
        every { item.ubicacionNombre } returns ubicacionNombre

        return item
    }

    private fun groupedItem(
        barcode: String,
        description: String,
        totalQuantity: Double,
        unitMeasure: String,
        ubicacionNombre: String,
        totalRows: Int
    ): InventoryGroupedRow {
        val item = mockk<InventoryGroupedRow>(relaxed = true)

        every { item.barcode } returns barcode
        every { item.description } returns description
        every { item.totalQuantity } returns totalQuantity
        every { item.unitMeasure } returns unitMeasure
        every { item.ubicacionNombre } returns ubicacionNombre
        every { item.totalRows } returns totalRows

        return item
    }

    private fun assertTagExists(tag: String) {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
    }

    private fun assertTagDoesNotExist(tag: String) {
        val nodes = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertEquals(0, nodes.size)
    }

    private fun assertTextExists(text: String) {
        composeRule.onNodeWithText(text).fetchSemanticsNode()
    }
}