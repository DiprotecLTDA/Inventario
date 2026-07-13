package com.diprotec.inventario.ui.synclog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.data.local.entity.SyncLogEntity
import com.diprotec.inventario.data.repository.SyncLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncLogUiItem(
    val inventoryName: String,
    val captures: String,
    val status: String,
    val result: String,
    val mode: String,
    val eventType: String,
    val sentAt: String,
    val message: String?
)

@HiltViewModel
class SyncLogViewModel @Inject constructor(
    private val repository: SyncLogRepository
) : ViewModel() {

    val logs = repository.observeAll()
        .map { items -> items.map { it.toUiItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private fun SyncLogEntity.toUiItem(): SyncLogUiItem {
        return SyncLogUiItem(
            inventoryName = inventoryName,
            captures = capturesCount.toString(),
            status = translateStatus(inventoryStatus),
            result = translateResult(result),
            mode = translateMode(connectionMode),
            eventType = translateEvent(eventType),
            sentAt = formatDate(sentAt),
            message = message
        )
    }

    private fun translateStatus(value: String): String =
        when (value) {
            "PENDING" -> "Pendiente"
            "FINISHED" -> "Finalizado"
            "EXPIRED" -> "Expirado"
            else -> value
        }

    private fun translateResult(value: String): String =
        when (value) {
            "ENVIADO" -> "Enviado"
            "ERROR" -> "Error"
            else -> value
        }

    private fun translateMode(value: String): String =
        when (value) {
            "ONLINE_API" -> "Online/API"
            "LOCAL_ROOM" -> "Room local"
            else -> value
        }

    private fun translateEvent(value: String): String =
        when (value) {
            "CAPTURES_SENT" -> "Capturas enviadas"
            "CAPTURES_FAILED" -> "Error al enviar capturas"
            "INVENTORY_FINISHED" -> "Inventario finalizado"
            "FINISH_FAILED" -> "Error al finalizar"
            else -> value
        }

    private fun formatDate(value: Long): String {
        return SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss",
            Locale("es", "CL")
        ).format(Date(value))
    }
}