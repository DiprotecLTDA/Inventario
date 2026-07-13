package com.diprotec.inventario.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.network.AppConnectionMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectionStatusViewModel @Inject constructor(
    private val monitor: AppConnectionMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(AppConnectionMode.CHECKING)
    val state: StateFlow<AppConnectionMode> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitor.observeMode().collect { mode ->
                _state.value = mode
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AppConnectionMode.CHECKING
            _state.value = monitor.checkMode()
        }
    }
}