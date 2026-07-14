package com.diprotec.inventario.ui.syncstatus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.theme.StatusChip
import com.diprotec.inventario.ui.theme.StatusError
import com.diprotec.inventario.ui.theme.StatusOnline
import com.diprotec.inventario.ui.theme.StatusWarning

@Composable
fun WorkerTrafficLight(
    modifier: Modifier = Modifier,
    vm: WorkerStatusViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    val color = when (state) {
        WorkerSyncState.SYNCING -> StatusOnline
        WorkerSyncState.WAITING -> StatusWarning
        WorkerSyncState.STOPPED -> StatusError
    }

    val shortValue = when (state) {
        WorkerSyncState.SYNCING -> "⟳"
        WorkerSyncState.WAITING -> "⏳"
        WorkerSyncState.STOPPED -> "⛔"
    }

    StatusChip(
        modifier = modifier,
        dotColor = color,
        title = "Sincronización",
        value = shortValue,
        valueTextStyle = MaterialTheme.typography.bodyMedium,
        valueTextAlign = TextAlign.Center
    )
}
