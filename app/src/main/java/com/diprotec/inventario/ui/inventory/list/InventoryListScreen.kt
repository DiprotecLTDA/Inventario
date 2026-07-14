package com.diprotec.inventario.ui.inventory.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.data.local.entity.InventoryItemEntity
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.StatusError
import com.diprotec.inventario.ui.theme.InventoryTopBar
import com.diprotec.inventario.ui.theme.OutlinedInfoCard
import com.diprotec.inventario.ui.theme.SegmentedToggle
import com.diprotec.inventario.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun InventoryListScreen(
    inventoryId: Long,
    onBack: () -> Unit,
    viewModel: InventoryListViewModel = hiltViewModel()
) {
    val stateFlow = remember(inventoryId) {
        viewModel.uiState(inventoryId)
    }

    val uiState by stateFlow.collectAsState()
    var itemToDelete by remember { mutableStateOf<InventoryItemEntity?>(null) }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text("Eliminar captura")
            },
            text = {
                Text(
                    text = "¿Desea eliminar la captura del producto ${item.barcode}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(
                            inventoryId = inventoryId,
                            itemId = item.id
                        )
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_inventory_item")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_inventory_item")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("inventory_list_screen")
    ) {
        InventoryTopBar(title = "LISTADO DE CAPTURAS")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "Inventario: ${uiState.inventoryName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("inventory_name")
            )

            if (!uiState.canDeleteItems && uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Inventario finalizado: las capturas solo se pueden visualizar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.testTag("inventory_readonly_message")
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = uiState.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusError,
                    modifier = Modifier.testTag("inventory_error_message")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            InventoryGroupedSelector(
                isGrouped = uiState.isGrouped,
                onUngroupedClick = {
                    viewModel.setGrouped(false)
                },
                onGroupedClick = {
                    viewModel.setGrouped(true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isGrouped) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_grouped_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.groupedItems) { item ->
                        InventoryGroupedCard(
                            barcode = item.barcode,
                            description = item.description,
                            quantity = item.totalQuantity,
                            unitMeasure = item.unitMeasure,
                            ubicacionNombre = item.ubicacionNombre,
                            totalRows = item.totalRows,
                            modifier = Modifier.testTag("inventory_grouped_item_${item.barcode}")
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_ungrouped_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.ungroupedItems,
                        key = { it.id }
                    ) { item ->
                        InventoryItemCard(
                            barcode = item.barcode,
                            description = item.description,
                            quantity = item.quantity,
                            unitMeasure = item.unitMeasure,
                            ubicacionNombre = item.ubicacionNombre,
                            onClick = {
                                if (uiState.canDeleteItems) {
                                    itemToDelete = item
                                }
                            },
                            modifier = Modifier.testTag("inventory_item_${item.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryGroupedSelector(
    isGrouped: Boolean,
    onUngroupedClick: () -> Unit,
    onGroupedClick: () -> Unit
) {
    SegmentedToggle(
        firstText = "Desagrupar",
        secondText = "Agrupar",
        firstSelected = !isGrouped,
        onFirstClick = onUngroupedClick,
        onSecondClick = onGroupedClick,
        modifier = Modifier.fillMaxWidth(),
        firstOptionModifier = Modifier.testTag("selector_ungrouped"),
        secondOptionModifier = Modifier.testTag("selector_grouped")
    )
}

@Composable
private fun InventoryItemCard(
    barcode: String,
    description: String,
    quantity: Double,
    unitMeasure: String,
    ubicacionNombre: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedInfoCard(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Código: $barcode",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Descripción: $description",
                color = TextPrimary
            )

            Text(
                text = "Cantidad: ${formatQuantity(quantity)}",
                color = TextPrimary
            )

            Text(
                text = "Unidad: $unitMeasure",
                color = TextPrimary
            )

            Text(
                text = "Ubicación: $ubicacionNombre",
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun InventoryGroupedCard(
    barcode: String,
    description: String,
    quantity: Double,
    unitMeasure: String,
    ubicacionNombre: String,
    totalRows: Int,
    modifier: Modifier = Modifier
) {
    OutlinedInfoCard(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Código: $barcode",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Descripción: $description",
                color = TextPrimary
            )

            Text(
                text = "Cantidad total: ${formatQuantity(quantity)}",
                color = TextPrimary
            )

            Text(
                text = "Unidad: $unitMeasure",
                color = TextPrimary
            )

            Text(
                text = "Ubicación: $ubicacionNombre",
                color = TextPrimary
            )

            Text(
                text = "Registros agrupados: $totalRows",
                color = TextPrimary
            )
        }
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.3f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
}
