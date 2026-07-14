package com.diprotec.inventario.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.components.StatusChip
import com.diprotec.inventario.ui.connection.AppConnectionMode
import com.diprotec.inventario.ui.connection.ConnectionModeIndicator
import com.diprotec.inventario.ui.syncstatus.WorkerTrafficLight
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.BrandPrimary
import com.diprotec.inventario.ui.theme.Dimens
import com.diprotec.inventario.ui.theme.InventoryMenuButton
import com.diprotec.inventario.ui.theme.TextPrimary
import com.diprotec.inventario.ui.theme.White

@Composable
fun MainMenuScreen(
    onRealizarInventario: () -> Unit,
    onContinuarInventario: () -> Unit,
    onHistorialEnvios: () -> Unit,
    onConsumoDatos: () -> Unit,
    onAcercaDe: () -> Unit,
    onSalir: () -> Unit,
    viewModel: MainMenuViewModel = hiltViewModel()
) {
    val currentUsername by viewModel.currentUsername.collectAsState()
    val pendingSyncState by viewModel.pendingSyncState
    val sessionRemainingText by viewModel.sessionRemainingText

    var showLogoutDialog by remember { mutableStateOf(false) }

    val showDataUsage = currentUsername == "76001910-0"

    val showWorkerTrafficLight =
        pendingSyncState.syncing ||
                (
                        pendingSyncState.connectionMode != AppConnectionMode.ONLINE_API &&
                                pendingSyncState.connectionMode != AppConnectionMode.CHECKING
                        )

    BackHandler {
        showLogoutDialog = true
    }

    val pendingSyncMessage =
        pendingSyncState.successMessage ?: pendingSyncState.errorMessage

    LaunchedEffect(pendingSyncMessage) {
        pendingSyncMessage?.let {
            AppFloatingMessage.info(it)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.spaceXl, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    ConnectionModeIndicator()

                    Spacer(modifier = Modifier.size(Dimens.spaceXs))

                    SessionTimeIndicator(
                        value = sessionRemainingText
                    )
                }

                if (showWorkerTrafficLight) {
                    Spacer(modifier = Modifier.size(Dimens.spaceS))

                    WorkerTrafficLight()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 14.dp, bottom = Dimens.spaceS),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    InventoryMenuButton(
                        text = "Realizar inventario",
                        icon = Icons.Default.Inventory2,
                        onClick = onRealizarInventario,
                        modifier = Modifier.weight(1f)
                    )

                    InventoryMenuButton(
                        text = "Continuar inventario",
                        icon = Icons.Default.QrCodeScanner,
                        onClick = onContinuarInventario,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    InventoryMenuButton(
                        text = "Sincronizar pendientes",
                        icon = Icons.Default.PlaylistAddCheckCircle,
                        onClick = viewModel::syncPendingInventories,
                        enabled = pendingSyncState.canSyncPending,
                        loading = pendingSyncState.syncing,
                        modifier = Modifier.weight(1f)
                    )

                    InventoryMenuButton(
                        text = "Acerca de",
                        icon = Icons.Default.Help,
                        onClick = onAcercaDe,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    InventoryMenuButton(
                        text = "Historial de envíos",
                        icon = Icons.Default.History,
                        onClick = onHistorialEnvios,
                        modifier = Modifier.weight(1f)
                    )

                    InventoryMenuButton(
                        text = "Salir",
                        icon = Icons.Default.Logout,
                        onClick = onSalir,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showDataUsage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        InventoryMenuButton(
                            text = "Consumo de datos",
                            icon = Icons.Default.DataUsage,
                            onClick = onConsumoDatos,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (pendingSyncState.syncing) {
            BlockingPendingSyncOverlay()
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLogoutDialog = false
                },
                title = {
                    Text("Cerrar sesión")
                },
                text = {
                    Text("¿Desea cerrar sesión?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onSalir()
                        }
                    ) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                        }
                    ) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
private fun SessionTimeIndicator(
    value: String,
    modifier: Modifier = Modifier
) {
    StatusChip(
        dotColor = BrandPrimary,
        title = "Sesión",
        value = value,
        modifier = modifier
    )
}

@Composable
private fun BlockingPendingSyncOverlay() {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceXxxl),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = Dimens.elevationL
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Actualizando lista",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(Dimens.spaceM))

                Text(
                    text = "Sincronizando inventarios pendientes…",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.size(18.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(Dimens.spaceM))

                Text(
                    text = "Por favor espera",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
