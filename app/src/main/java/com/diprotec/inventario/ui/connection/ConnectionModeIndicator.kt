package com.diprotec.inventario.ui.connection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.components.StatusChip
import com.diprotec.inventario.ui.theme.StatusError
import com.diprotec.inventario.ui.theme.StatusOnline
import com.diprotec.inventario.ui.theme.StatusWarning

@Composable
fun ConnectionModeIndicator(
    modifier: Modifier = Modifier,
    vm: ConnectionStatusViewModel = hiltViewModel()
) {
    val mode by vm.state.collectAsState()

    val color = when (mode) {
        AppConnectionMode.ONLINE_API -> StatusOnline
        AppConnectionMode.CHECKING -> StatusWarning
        AppConnectionMode.LOCAL_ROOM -> StatusError
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
