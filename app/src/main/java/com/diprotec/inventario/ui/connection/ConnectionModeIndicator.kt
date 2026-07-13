package com.diprotec.inventario.ui.connection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.theme.StatusChecking
import com.diprotec.inventario.ui.theme.StatusChip
import com.diprotec.inventario.ui.theme.StatusOffline
import com.diprotec.inventario.ui.theme.StatusOnline

@Composable
fun ConnectionModeIndicator(
    modifier: Modifier = Modifier,
    vm: ConnectionStatusViewModel = hiltViewModel()
) {
    val mode by vm.state.collectAsState()

    val color = when (mode) {
        AppConnectionMode.ONLINE_API -> StatusOnline
        AppConnectionMode.CHECKING -> StatusChecking
        AppConnectionMode.LOCAL_ROOM -> StatusOffline
    }

    val label = when (mode) {
        AppConnectionMode.ONLINE_API -> "Online"
        AppConnectionMode.CHECKING -> "Verificando"
        AppConnectionMode.LOCAL_ROOM -> "Room"
    }

    StatusChip(
        modifier = modifier,
        dotColor = color,
        title = "Conexión",
        value = label
    )
}
