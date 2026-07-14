package com.diprotec.inventario.ui.datausage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.data.local.dao.NetworkUsageGroupRow
import com.diprotec.inventario.data.repository.NetworkUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DataUsageUiState(
    val loading: Boolean = true,
    val today: String = "0 B",
    val last7Days: String = "0 B",
    val todayCalls: Int = 0,
    val averagePerCall: String = "0 B",
    val byOperation: List<NetworkUsageGroupRow> = emptyList(),
    val bySource: List<NetworkUsageGroupRow> = emptyList(),
    val byEndpoint: List<NetworkUsageGroupRow> = emptyList(),
    val successMessage: String? = null
)

@HiltViewModel
class DataUsageViewModel @Inject constructor(
    private val repository: NetworkUsageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataUsageUiState())
    val state: StateFlow<DataUsageUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                successMessage = null
            )

            val summary = repository.loadSummary()

            _state.value = DataUsageUiState(
                loading = false,
                today = formatBytes(summary.todayBytes),
                last7Days = formatBytes(summary.last7DaysBytes),
                todayCalls = summary.todayCallCount,
                averagePerCall = formatBytes(summary.averageBytesPerCallToday),
                byOperation = summary.byOperation,
                bySource = summary.bySource,
                byEndpoint = summary.byEndpoint
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAll()
            _state.value = _state.value.copy(
                successMessage = AppMessages.DataUsage.REGISTROS_LIMPIADOS
            )
            refresh()
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"

        val kb = bytes / 1024.0
        if (kb < 1024.0) {
            return String.format(Locale.US, "%.2f KB", kb)
        }

        val mb = kb / 1024.0
        if (mb < 1024.0) {
            return String.format(Locale.US, "%.2f MB", mb)
        }

        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null)
    }
}
