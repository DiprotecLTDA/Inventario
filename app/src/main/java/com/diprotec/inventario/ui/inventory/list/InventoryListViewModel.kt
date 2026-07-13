package com.diprotec.inventario.ui.inventory.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: InventoryRepository
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
                emit(repository.getInventoryById(inventoryId))
            }.flowOn(Dispatchers.IO)
        ) { isGrouped, ungrouped, grouped, inventory ->
            val inventoryStatus = inventory?.status.orEmpty()
            val canDeleteItems = inventoryStatus != InventoryStatus.FINISHED.name

            InventoryListUiState(
                isGrouped = isGrouped,
                inventoryName = inventory?.name.orEmpty(),
                inventoryStatus = inventoryStatus,
                canDeleteItems = canDeleteItems,
                ungroupedItems = ungrouped,
                groupedItems = grouped,
                errorMessage = if (inventory == null) "No se encontró el inventario" else null
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventoryListUiState()
        )
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