package com.diprotec.inventario.ui.inventory.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.auth.SuperAdminAccess
import com.diprotec.inventario.core.concurrency.SingleFlightGuard
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.repository.InventoryRemoteRepository
import com.diprotec.inventario.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InventarioAsignadoOption(
    val id: String,
    val descripcion: String,
    val fecha: String? = null,
    val hora: String? = null,
    val desde: String? = null,
    val hasta: String? = null,
    val rutAdministrador: String? = null,
    val rutEmpresa: String? = null
) {
    val inicioTexto: String
        get() = listOfNotNull(desde, hora)
            .joinToString(" ")
            .ifBlank { "Sin inicio" }

    val terminoTexto: String
        get() = listOfNotNull(hasta, hora)
            .joinToString(" ")
            .ifBlank { "Sin término" }
}

data class CreateInventoryUiState(
    val inventarios: List<InventarioAsignadoOption> = emptyList(),
    val selectedInventarioId: String = "",
    val selectedInventarioDescripcion: String = "",
    val selectedInicioTexto: String = "",
    val selectedTerminoTexto: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class CreateInventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val inventoryRemoteRepository: InventoryRemoteRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateInventoryUiState())
    val uiState: StateFlow<CreateInventoryUiState> = _uiState.asStateFlow()

    private val createGuard = SingleFlightGuard()

    init {
        observeInventariosAsignados()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeInventariosAsignados() {
        viewModelScope.launch {
            session.loginRut
                .flatMapLatest { rut ->
                    val rutUsuario = rut.orEmpty().trim()

                    if (rutUsuario.isBlank()) {
                        flowOf(emptyList<InventarioAsignadoOption>())
                    } else {
                        val isSuperAdmin = SuperAdminAccess.isSuperAdmin(rutUsuario)

                        val remotosFlow = if (isSuperAdmin) {
                            inventoryRemoteRepository.observeInventariosActivos()
                        } else {
                            inventoryRemoteRepository.observeInventariosAsignadosActivos(rutUsuario)
                        }

                        val localesFlow = if (isSuperAdmin) {
                            repository.observeAllInventories()
                        } else {
                            repository.observeAllInventoriesByUsuario(rutUsuario)
                        }

                        combine(
                            remotosFlow,
                            localesFlow
                        ) { remotos, locales ->

                            val remoteIdsYaCreados = locales
                                .map { it.remoteInventoryId.trim() }
                                .filter { it.isNotBlank() }
                                .toSet()

                            remotos
                                .filter { remoto ->
                                    val remoteId = remoto.id.trim()

                                    remoteId.isNotBlank() &&
                                            !remoteIdsYaCreados.contains(remoteId) &&
                                            !isExpired(remoto.hasta)
                                }
                                .map {
                                    InventarioAsignadoOption(
                                        id = it.id.trim(),
                                        descripcion = it.descripcion.orEmpty(),
                                        fecha = it.fecha,
                                        hora = it.hora,
                                        desde = it.desde,
                                        hasta = it.hasta,
                                        rutAdministrador = it.rutAdministrador,
                                        rutEmpresa = it.rutEmpresa
                                    )
                                }
                                .filter {
                                    it.id.isNotBlank() && it.descripcion.isNotBlank()
                                }
                        }
                    }
                }
                .flowOn(Dispatchers.Default)
                .collectLatest { options ->

                    val currentSelectedId = _uiState.value.selectedInventarioId
                    val selected = options.firstOrNull { it.id == currentSelectedId }
                        ?: options.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        inventarios = options,
                        selectedInventarioId = selected?.id.orEmpty(),
                        selectedInventarioDescripcion = selected?.descripcion.orEmpty(),
                        selectedInicioTexto = selected?.inicioTexto.orEmpty(),
                        selectedTerminoTexto = selected?.terminoTexto.orEmpty(),
                        errorMessage = if (options.isEmpty()) {
                            AppMessages.Inventory.SIN_INVENTARIOS_DISPONIBLES
                        } else {
                            null
                        }
                    )
                }
        }
    }

    fun onInventarioSelected(id: String) {
        val selected = _uiState.value.inventarios.firstOrNull {
            it.id == id.trim()
        } ?: return

        _uiState.value = _uiState.value.copy(
            selectedInventarioId = selected.id,
            selectedInventarioDescripcion = selected.descripcion,
            selectedInicioTexto = selected.inicioTexto,
            selectedTerminoTexto = selected.terminoTexto,
            errorMessage = null
        )
    }

    fun createSelectedInventory(onCreated: (Long) -> Unit) {
        val rutUsuario = session.loginRut.value.orEmpty().trim()

        if (rutUsuario.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = AppMessages.Inventory.SIN_USUARIO
            )
            return
        }

        val selected = _uiState.value.inventarios
            .firstOrNull { it.id == _uiState.value.selectedInventarioId }

        if (selected == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = AppMessages.Inventory.DEBE_SELECCIONAR_INVENTARIO
            )
            return
        }

        if (!createGuard.tryAcquire()) return

        viewModelScope.launch {
            try {
                val inventoryId = withContext(Dispatchers.IO) {
                    repository.createInventoryFromRemote(
                        remote = InventoryRemoteEntity(
                            id = selected.id.trim(),
                            descripcion = selected.descripcion,
                            fecha = selected.fecha,
                            hora = selected.hora,
                            desde = selected.desde,
                            hasta = selected.hasta,
                            rutAdministrador = selected.rutAdministrador,
                            vigente = false,
                            rutEmpresa = selected.rutEmpresa
                        ),
                        rutUsuario = rutUsuario
                    )
                }

                onCreated(inventoryId)
            } finally {
                createGuard.release()
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun isExpired(hasta: String?): Boolean {
        if (hasta.isNullOrBlank()) return false

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            isLenient = false
        }

        val limit = runCatching { formatter.parse(hasta) }.getOrNull()
            ?: return false

        val today = formatter.parse(formatter.format(Date())) ?: Date()

        return today.after(limit)
    }
}
