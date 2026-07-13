package com.diprotec.inventario.ui.inventory.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.auth.SuperAdminAccess
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.data.local.entity.InventoryEntity
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import com.diprotec.inventario.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PendingInventoryFilter {
    PENDING,
    FINISHED
}

@HiltViewModel
class PendingInventoriesViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val session: SessionManager
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(PendingInventoryFilter.PENDING)

    val filter: StateFlow<PendingInventoryFilter> = selectedFilter

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pendingInventories =
        session.loginRut.flatMapLatest { rut ->
            val rutUsuario = rut.orEmpty().trim()

            if (rutUsuario.isBlank()) {
                flowOf(emptyList())
            } else if (SuperAdminAccess.isSuperAdmin(rutUsuario)) {
                repository.observeInventoriesByStatus(
                    InventoryStatus.PENDING
                )
            } else {
                repository.observeInventoriesByStatusAndUsuario(
                    InventoryStatus.PENDING,
                    rutUsuario
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val finishedInventories =
        session.loginRut.flatMapLatest { rut ->
            val rutUsuario = rut.orEmpty().trim()

            if (rutUsuario.isBlank()) {
                flowOf(emptyList())
            } else if (SuperAdminAccess.isSuperAdmin(rutUsuario)) {
                repository.observeInventoriesByStatus(
                    InventoryStatus.FINISHED
                )
            } else {
                repository.observeInventoriesByStatusAndUsuario(
                    InventoryStatus.FINISHED,
                    rutUsuario
                )
            }
        }

    val inventories: StateFlow<List<InventoryEntity>> =
        combine(
            selectedFilter,
            pendingInventories,
            finishedInventories
        ) { filter, pending, finished ->
            val source = when (filter) {
                PendingInventoryFilter.PENDING -> pending
                PendingInventoryFilter.FINISHED -> finished
            }

            source.filter { repository.isInventoryVisible(it) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            session.loginRut.collect { rut ->
                val usuario = rut.orEmpty().trim()

                if (usuario.isNotBlank()) {
                    if (SuperAdminAccess.isSuperAdmin(usuario)) {
                        repository.expirePendingInventories()
                    } else {
                        repository.expirePendingInventoriesByUsuario(usuario)
                    }
                }
            }
        }
    }

    fun setFilter(value: PendingInventoryFilter) {
        selectedFilter.value = value

        viewModelScope.launch(Dispatchers.IO) {
            val usuario = session.loginRut.value.orEmpty().trim()

            if (usuario.isNotBlank()) {
                if (SuperAdminAccess.isSuperAdmin(usuario)) {
                    repository.expirePendingInventories()
                } else {
                    repository.expirePendingInventoriesByUsuario(usuario)
                }
            }
        }
    }
}