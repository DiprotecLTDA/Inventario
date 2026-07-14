package com.diprotec.inventario.ui.inventory.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.BorderGray
import com.diprotec.inventario.ui.theme.Dimens
import com.diprotec.inventario.ui.theme.StatusError
import com.diprotec.inventario.ui.components.InventoryTopBar
import com.diprotec.inventario.ui.components.inventoryTextFieldColors
import com.diprotec.inventario.ui.theme.InventoryMenuButton
import com.diprotec.inventario.ui.theme.LabelGray
import com.diprotec.inventario.ui.theme.TextPrimary
import com.diprotec.inventario.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInventoryScreen(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateInventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var expandedInventario by rememberSaveable { mutableStateOf(false) }

    val canContinue =
        uiState.selectedInventarioId.isNotBlank() &&
                uiState.selectedInventarioDescripcion.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        InventoryTopBar(title = "INVENTARIO")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.spaceXl, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedInventario,
                    onExpandedChange = { expandedInventario = !expandedInventario },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.selectedInventarioDescripcion,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = {
                            FloatingFieldLabel("Seleccione inventario")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedInventario
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(min = Dimens.fieldTallHeight),
                        shape = MaterialTheme.shapes.medium,
                        colors = inventoryTextFieldColors()
                    )

                    DropdownMenu(
                        expanded = expandedInventario,
                        onDismissRequest = { expandedInventario = false }
                    ) {
                        uiState.inventarios.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.descripcion) },
                                onClick = {
                                    viewModel.onInventarioSelected(item.id)
                                    expandedInventario = false
                                }
                            )
                        }
                    }
                }

                InventoryDateInfo(
                    title = "Válido desde:",
                    value = uiState.selectedInicioTexto.ifBlank { "Sin inicio" }
                )

                InventoryDateInfo(
                    title = "Fecha máxima para realizar:",
                    value = uiState.selectedTerminoTexto.ifBlank { "Sin término" }
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = StatusError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                InventoryMenuButton(
                    text = "Aceptar",
                    icon = Icons.Default.CheckCircle,
                    enabled = canContinue,
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
            }
        }
    }
}

@Composable
private fun InventoryDateInfo(
    title: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.fieldTallHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceS)
                .background(White, MaterialTheme.shapes.medium)
                .border(Dimens.borderThin, BorderGray, MaterialTheme.shapes.medium)
                .padding(horizontal = Dimens.spaceL, vertical = 18.dp)
        ) {
            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 28.dp)
                .background(Background)
                .padding(horizontal = Dimens.spaceXxs)
        ) {
            FloatingFieldLabel(title)
        }
    }
}

@Composable
private fun FloatingFieldLabel(
    text: String
) {
    Text(
        text = text,
        color = LabelGray,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Background)
            .padding(horizontal = Dimens.spaceXxs)
    )
}
