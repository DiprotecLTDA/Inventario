package com.diprotec.inventario.ui.inventory.pending

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.data.local.entity.InventoryEntity
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.InventoryTopBar
import com.diprotec.inventario.ui.theme.OutlinedInfoCard
import com.diprotec.inventario.ui.theme.SegmentedToggle
import com.diprotec.inventario.ui.theme.TextPrimary

@Composable
fun PendingInventoriesScreen(
    onBack: () -> Unit,
    onOpenPendingInventory: (Long) -> Unit,
    onOpenFinishedInventory: (Long) -> Unit,
    viewModel: PendingInventoriesViewModel = hiltViewModel()
) {
    val items by viewModel.inventories.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        InventoryTopBar(title = "LISTADO DE INVENTARIOS")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            PendingFinishedSelector(
                filter = filter,
                onPendingClick = {
                    viewModel.setFilter(PendingInventoryFilter.PENDING)
                },
                onFinishedClick = {
                    viewModel.setFilter(PendingInventoryFilter.FINISHED)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    InventoryCard(
                        item = item,
                        onClick = {
                            if (item.status == InventoryStatus.FINISHED.name) {
                                onOpenFinishedInventory(item.id)
                            } else {
                                onOpenPendingInventory(item.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingFinishedSelector(
    filter: PendingInventoryFilter,
    onPendingClick: () -> Unit,
    onFinishedClick: () -> Unit
) {
    SegmentedToggle(
        firstText = "Pendientes",
        secondText = "Finalizados",
        firstSelected = filter == PendingInventoryFilter.PENDING,
        onFirstClick = onPendingClick,
        onSecondClick = onFinishedClick,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InventoryCard(
    item: InventoryEntity,
    onClick: () -> Unit
) {
    OutlinedInfoCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Inicio: ${item.desde.orEmpty()} ${item.hora.orEmpty()}",
                color = TextPrimary
            )

            Text(
                text = "Término: ${item.hasta.orEmpty()} ${item.hora.orEmpty()}",
                color = TextPrimary
            )

            Text(
                text = "Estado: ${formatInventoryStatus(item.status)}",
                color = TextPrimary
            )
        }
    }
}

private fun formatInventoryStatus(status: String): String {
    return when (status) {
        InventoryStatus.PENDING.name -> "Pendiente"
        InventoryStatus.FINISHED.name -> "Finalizado"
        else -> status
    }
}
