package com.diprotec.inventario.ui.inventory.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.data.local.dao.RuleDao
import com.diprotec.inventario.data.local.entity.InventoryItemEntity
import com.diprotec.inventario.data.local.inventory.InventoryGroupedRow
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import com.diprotec.inventario.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryListUiState(
    val isGrouped: Boolean = false,
    val inventoryName: String = "",
    val inventoryStatus: String = "",
    val canDeleteItems: Boolean = true,
    val ungroupedItems: List<InventoryItemEntity> = emptyList(),
    val groupedItems: List<InventoryGroupedRow> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val ruleDao: RuleDao,
    private val settings: SettingsManager,
    private val session: SessionManager
) : ViewModel() {

    private val groupedMode = MutableStateFlow(false)

    fun setGrouped(value: Boolean) {
        groupedMode.value = value
    }

    fun uiState(inventoryId: Long): StateFlow<InventoryListUiState> {
        return combine(
            groupedMode,
            repository.observeInventoryItems(inventoryId),
            repository.observeGroupedInventoryItems(inventoryId),
            flow {
                val inventory = repository.getInventoryById(inventoryId)
                val ruleAllowsDelete = ruleAllowsDeleteForCurrentUser()
                emit(inventory to ruleAllowsDelete)
            }.flowOn(Dispatchers.IO)
        ) { isGrouped, ungrouped, grouped, inventoryAndRule ->
            val (inventory, ruleAllowsDelete) = inventoryAndRule
            val inventoryStatus = inventory?.status.orEmpty()
            val canDeleteItems = inventoryStatus != InventoryStatus.FINISHED.name && ruleAllowsDelete

            InventoryListUiState(
                isGrouped = isGrouped,
                inventoryName = inventory?.name.orEmpty(),
                inventoryStatus = inventoryStatus,
                canDeleteItems = canDeleteItems,
                ungroupedItems = ungrouped,
                groupedItems = grouped,
                errorMessage = if (inventory == null) AppMessages.Inventory.NO_ENCONTRADO else null
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventoryListUiState()
        )
    }

    /**
     * Un usuario puede eliminar capturas ya sincronizadas si la regla de negocio de su perfil
     * (o, en su defecto, la regla genérica de la empresa) lo autoriza. Si la empresa no tiene
     * ninguna regla configurada (catálogo `reglas` vacío o no aplicable), se permite por
     * defecto para no bloquear el flujo de un cliente que no usa ese catálogo.
     */
    private suspend fun ruleAllowsDeleteForCurrentUser(): Boolean {
        val rutEmpresa = settings.empresaRut.value.trim()
        if (rutEmpresa.isBlank()) return true

        val reglas = ruleDao.findByEmpresa(rutEmpresa)
        if (reglas.isEmpty()) return true

        val perfilId = session.perfilId.value?.toString()

        val regla = reglas.firstOrNull { it.perfil == perfilId }
            ?: reglas.firstOrNull { it.perfil.isNullOrBlank() }
            ?: return true

        return regla.vigente && regla.eliminaEnviados == RULE_ELIMINA_ENVIADOS_PERMITIDO
    }

    private companion object {
        // Convención del backend: "0" = la regla permite eliminar capturas ya enviadas.
        private const val RULE_ELIMINA_ENVIADOS_PERMITIDO = "0"
    }

    fun deleteItem(
        inventoryId: Long,
        itemId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInventoryItem(
                inventoryId = inventoryId,
                itemId = itemId
            )
        }
    }
}
