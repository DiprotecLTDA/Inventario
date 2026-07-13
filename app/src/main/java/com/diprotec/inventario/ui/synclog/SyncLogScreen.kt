package com.diprotec.inventario.ui.synclog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.InventoryTopBar
import com.diprotec.inventario.ui.theme.OutlinedInfoCard
import com.diprotec.inventario.ui.theme.TextPrimary

@Composable
fun SyncLogScreen(
    onBack: () -> Unit,
    vm: SyncLogViewModel = hiltViewModel()
) {
    val logs by vm.logs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        InventoryTopBar(
            title = "HISTORIAL DE ENVÍOS",
            actions = {
                IconButton(
                    onClick = {
                        vm.clearAll()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpiar historial",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = "No hay envíos registrados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logs) { item ->
                        SyncLogCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncLogCard(
    item: SyncLogUiItem
) {
    OutlinedInfoCard(
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = item.inventoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "Evento: ${item.eventType}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Capturas: ${item.captures}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Estado inventario: ${item.status}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Resultado: ${item.result}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Modo: ${item.mode}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Hora: ${item.sentAt}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            if (!item.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}
