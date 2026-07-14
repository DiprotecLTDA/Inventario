package com.diprotec.inventario.ui.inventory.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.data.local.entity.InventoryEntity
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import com.diprotec.inventario.data.repository.InventoryRepository
import com.diprotec.inventario.data.repository.UnitMeasureRepository
import com.diprotec.inventario.service.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_BARCODE_LENGTH = 50

enum class CaptureMode {
    UNIT,
    QUANTITY
}

data class UnidadMedidaOption(
    val id: String,
    val nombre: String
)

data class UbicacionOption(
    val id: String,
    val nombre: String
)

data class CaptureInventoryUiState(
    val scanMode: CaptureMode = CaptureMode.UNIT,
    val barcode: String = "",
    val quantityInput: String = "",

    val selectedUnitId: String = "",
    val selectedUnitName: String = "",

    val selectedUbicacionId: String = "",
    val selectedUbicacionName: String = "",

    val description: String = "",

    val units: List<UnidadMedidaOption> = emptyList(),
    val ubicaciones: List<UbicacionOption> = emptyList(),

    val errorMessage: String? = null,
    val successMessage: String? = null,
    val successMessageId: Long = 0L,

    val inventoryName: String = "",
    val inventoryStatus: String = InventoryStatus.PENDING.name
)

@HiltViewModel
class CaptureInventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val unitMeasureRepository: UnitMeasureRepository,
    private val session: SessionManager,
    private val syncService: SyncService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureInventoryUiState())
    val uiState: StateFlow<CaptureInventoryUiState> = _uiState.asStateFlow()

    init {
        observeUnidadMedidas()
        observeUbicaciones()
    }

    private fun observeUnidadMedidas() {
        viewModelScope.launch {
            unitMeasureRepository.observeUnidadMedidas()
                .collectLatest { list ->
                    val ordenadas = list
                        .filter { !it.vigente }
                        .sortedByDescending { it.predeterminado }
                        .map {
                            UnidadMedidaOption(
                                id = it.id,
                                nombre = it.nombre.orEmpty()
                            )
                        }
                        .filter {
                            it.id.isNotBlank() && it.nombre.isNotBlank()
                        }

                    val currentSelectedId = _uiState.value.selectedUnitId
                    val defaultUnit = ordenadas.firstOrNull { it.id == currentSelectedId }
                        ?: ordenadas.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        units = ordenadas,
                        selectedUnitId = defaultUnit?.id.orEmpty(),
                        selectedUnitName = defaultUnit?.nombre.orEmpty()
                    )
                }
        }
    }

    private fun observeUbicaciones() {
        viewModelScope.launch {
            repository.observeUbicacionesActivas()
                .collectLatest { list ->
                    val options = list
                        .map {
                            UbicacionOption(
                                id = it.id,
                                nombre = it.nombre.orEmpty()
                            )
                        }
                        .filter {
                            it.id.isNotBlank() && it.nombre.isNotBlank()
                        }

                    val currentSelectedId = _uiState.value.selectedUbicacionId
                    val selected = options.firstOrNull { it.id == currentSelectedId }
                        ?: options.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        ubicaciones = options,
                        selectedUbicacionId = selected?.id.orEmpty(),
                        selectedUbicacionName = selected?.nombre.orEmpty()
                    )
                }
        }
    }

    fun loadInventory(inventoryId: Long) {
        viewModelScope.launch {
            val inventory: InventoryEntity? = withContext(Dispatchers.IO) {
                repository.getInventoryById(inventoryId)
            }

            if (inventory == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = AppMessages.Inventory.NO_ENCONTRADO,
                    successMessage = null
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                inventoryName = inventory.name,
                inventoryStatus = inventory.status,
                errorMessage = null
            )
        }
    }

    fun onScanModeChanged(mode: CaptureMode) {
        _uiState.value = _uiState.value.copy(
            scanMode = mode,
            quantityInput = if (mode == CaptureMode.UNIT) "" else _uiState.value.quantityInput,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onQuantityChanged(value: String) {
        val normalized = value.replace(",", ".")

        val clean = buildString {
            var hasDot = false

            normalized.forEach { char ->
                when {
                    char.isDigit() -> append(char)
                    char == '.' && !hasDot -> {
                        append(char)
                        hasDot = true
                    }
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            quantityInput = clean,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onUnitSelected(unitId: String) {
        val unit = _uiState.value.units.firstOrNull { it.id == unitId }
            ?: return

        _uiState.value = _uiState.value.copy(
            selectedUnitId = unit.id,
            selectedUnitName = unit.nombre,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onUbicacionSelected(ubicacionId: String) {
        val ubicacion = _uiState.value.ubicaciones.firstOrNull { it.id == ubicacionId }
            ?: return

        _uiState.value = _uiState.value.copy(
            selectedUbicacionId = ubicacion.id,
            selectedUbicacionName = ubicacion.nombre,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onManualBarcodeChanged(value: String) {
        val clean = normalizeBarcodeInput(value)

        _uiState.value = _uiState.value.copy(
            barcode = clean,
            errorMessage = null,
            successMessage = null
        )
    }

    fun lookupManualBarcodeInfo() {
        val state = _uiState.value

        if (state.scanMode != CaptureMode.UNIT) return

        val barcode = normalizeBarcodeInput(state.barcode)

        if (barcode.isBlank()) return

        if (state.inventoryStatus != InventoryStatus.PENDING.name) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.NO_ESTA_PENDIENTE,
                successMessage = null
            )
            return
        }

        viewModelScope.launch {
            val description = withContext(Dispatchers.IO) {
                repository.findDescriptionByBarcode(barcode)
            }

            _uiState.value = _uiState.value.copy(
                barcode = barcode,
                description = description.ifBlank { "Producto no registrado" },
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onBarcodeDetected(barcode: String) {
        val cleanBarcode = normalizeBarcodeInput(barcode)

        if (cleanBarcode.isBlank()) return

        if (_uiState.value.inventoryStatus != InventoryStatus.PENDING.name) {
            _uiState.value = _uiState.value.copy(
                errorMessage = AppMessages.Inventory.NO_ESTA_PENDIENTE,
                successMessage = null
            )
            return
        }

        viewModelScope.launch {
            val description = withContext(Dispatchers.IO) {
                repository.findDescriptionByBarcode(cleanBarcode)
            }

            _uiState.value = _uiState.value.copy(
                barcode = cleanBarcode,
                description = description.ifBlank { "Producto no registrado" },
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun registerDetectedBarcode(
        inventoryId: Long,
        barcode: String
    ) {
        val cleanBarcode = normalizeBarcodeInput(barcode)

        if (cleanBarcode.isBlank()) return

        viewModelScope.launch {
            val description = withContext(Dispatchers.IO) {
                repository.findDescriptionByBarcode(cleanBarcode)
            }

            _uiState.value = _uiState.value.copy(
                barcode = cleanBarcode,
                description = description.ifBlank { "Producto no registrado" },
                errorMessage = null,
                successMessage = null
            )

            registerCurrentScan(inventoryId)
        }
    }

    fun registerManualBarcode(inventoryId: Long) {
        val state = _uiState.value

        if (state.scanMode != CaptureMode.UNIT) return

        registerInventoryInternal(
            inventoryId = inventoryId
        )
    }

    fun registerCurrentScan(inventoryId: Long) {
        registerInventoryInternal(
            inventoryId = inventoryId
        )
    }

    private fun registerInventoryInternal(
        inventoryId: Long
    ) {
        val state = _uiState.value

        if (state.inventoryStatus != InventoryStatus.PENDING.name) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.NO_ESTA_PENDIENTE,
                successMessage = null
            )
            return
        }

        val barcode = normalizeBarcodeInput(state.barcode)

        if (barcode.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.DEBE_INGRESAR_CODIGO,
                successMessage = null
            )
            return
        }

        if (barcode.length > MAX_BARCODE_LENGTH) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.CODIGO_DEMASIADO_LARGO,
                successMessage = null
            )
            return
        }

        if (!barcode.all { it.isLetterOrDigit() }) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.CODIGO_INVALIDO,
                successMessage = null
            )
            return
        }

        if (state.selectedUbicacionId.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.DEBE_SELECCIONAR_UBICACION,
                successMessage = null
            )
            return
        }

        val quantity = when (state.scanMode) {
            CaptureMode.UNIT -> 1.0
            CaptureMode.QUANTITY -> {
                val value = state.quantityInput
                    .replace(",", ".")
                    .toDoubleOrNull()

                if (value == null || value <= 0.0) {
                    _uiState.value = state.copy(
                        errorMessage = AppMessages.Inventory.CANTIDAD_INVALIDA,
                        successMessage = null
                    )
                    return
                }

                value
            }
        }

        val unitId = state.selectedUnitId.trim()

        if (unitId.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.DEBE_SELECCIONAR_UNIDAD,
                successMessage = null
            )
            return
        }

        val rutUsuario = session.loginRut.value.orEmpty()

        if (rutUsuario.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = AppMessages.Inventory.SIN_USUARIO,
                successMessage = null
            )
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.registerInventoryItem(
                    inventoryId = inventoryId,
                    ubicacionId = state.selectedUbicacionId,
                    ubicacionNombre = state.selectedUbicacionName,
                    barcode = barcode,
                    quantity = quantity,
                    unitMeasure = state.selectedUnitName,
                    unitMeasureId = state.selectedUnitId,
                    rutUsuario = rutUsuario
                )
            }

            _uiState.value = _uiState.value.copy(
                barcode = barcode,
                description = state.description,
                quantityInput = state.quantityInput,
                errorMessage = null,
                successMessage = AppMessages.Inventory.PRODUCTO_REGISTRADO,
                successMessageId = System.nanoTime()
            )
        }
    }

    fun finalizeInventory(
        inventoryId: Long,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            val rutUsuario = session.loginRut.value.orEmpty()

            if (rutUsuario.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = AppMessages.Inventory.SIN_USUARIO,
                    successMessage = null
                )
                return@launch
            }

            if (_uiState.value.inventoryStatus == InventoryStatus.FINISHED.name) {
                onFinished()
                return@launch
            }

            withContext(Dispatchers.IO) {
                repository.finalizeInventory(inventoryId)
            }

            _uiState.value = _uiState.value.copy(
                inventoryStatus = InventoryStatus.FINISHED.name,
                successMessage = AppMessages.Inventory.FINALIZADO_LOCALMENTE,
                successMessageId = System.nanoTime(),
                errorMessage = null
            )

            val syncResult = withContext(Dispatchers.IO) {
                runCatching {
                    syncService.syncRegistroInventarios()
                    syncService.finishInventarioRemoto(
                        inventoryId = inventoryId,
                        usuarioRut = rutUsuario
                    )
                }
            }

            syncResult.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = AppMessages.Inventory.cierrePendiente(t.message),
                    successMessage = null
                )
            }

            onFinished()
        }
    }

    fun clearRegisteredProductInfo() {
        _uiState.value = _uiState.value.copy(
            barcode = "",
            description = "",
            quantityInput = "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    private fun normalizeBarcodeInput(value: String): String {
        return value
            .filter { it.isLetterOrDigit() }
            .take(MAX_BARCODE_LENGTH)
    }
}
