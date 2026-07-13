package com.diprotec.inventario.ui.inventory.capture

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.common.AppFloatingMessageHost
import com.diprotec.inventario.ui.scan.UnitechScan
import com.diprotec.inventario.ui.theme.AppPrimaryButton
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.BrandPrimary
import com.diprotec.inventario.ui.theme.BrandPrimaryDark
import com.diprotec.inventario.ui.theme.Error
import com.diprotec.inventario.ui.theme.InventoryMenuButton
import com.diprotec.inventario.ui.theme.LabelGray
import com.diprotec.inventario.ui.theme.TextPrimary
import com.diprotec.inventario.ui.theme.White
import com.diprotec.inventario.ui.theme.inventoryTextFieldColors
import kotlinx.coroutines.delay

private const val TAG_SCAN_CAPTURE = "SCAN_CAPTURE"
private const val PRODUCT_REGISTERED_MESSAGE_MS = 800L
private const val MAX_BARCODE_LENGTH = 50

private val FieldSpacing = 8.dp
private val LabelSpacing = 6.dp
private val LabelTopPadding = 10.dp
private val BottomContentPadding = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureInventoryScreen(
    inventoryId: Long,
    onBack: () -> Unit,
    onViewList: () -> Unit,
    onLeavePending: () -> Unit,
    onFinishInventory: () -> Unit,
    viewModel: CaptureInventoryViewModel = hiltViewModel(),
    enableScanner: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var scannerBuffer by rememberSaveable { mutableStateOf("") }
    var expandedMedida by rememberSaveable { mutableStateOf(false) }
    var expandedUbicacion by rememberSaveable { mutableStateOf(false) }
    var isManualBarcodeFocused by remember { mutableStateOf(false) }

    val scannerFocusRequester = remember { FocusRequester() }
    val barcodeFocusRequester = remember { FocusRequester() }
    val quantityFocusRequester = remember { FocusRequester() }
    val unitMeasureFocusRequester = remember { FocusRequester() }

    var scanVersion by remember { mutableIntStateOf(0) }

    fun restoreScannerFocus() {
        if (isManualBarcodeFocused) return

        runCatching {
            scannerFocusRequester.requestFocus()
            keyboardController?.hide()
        }.onFailure { error ->
            Log.d(TAG_SCAN_CAPTURE, "No se pudo restaurar foco scanner: ${error.message}")
        }
    }

    fun focusBarcodeField() {
        expandedMedida = false

        runCatching {
            barcodeFocusRequester.requestFocus()
            keyboardController?.hide()
        }.onFailure { error ->
            Log.d(TAG_SCAN_CAPTURE, "No se pudo enfocar código: ${error.message}")
        }
    }

    fun focusQuantityField() {
        expandedMedida = false

        runCatching {
            quantityFocusRequester.requestFocus()
        }.onFailure { error ->
            Log.d(TAG_SCAN_CAPTURE, "No se pudo enfocar cantidad: ${error.message}")
        }
    }

    fun registerManualBarcodeFromKeyboard() {
        if (uiState.scanMode != CaptureMode.UNIT) {
            focusQuantityField()
            return
        }

        if (uiState.barcode.trim().isBlank()) return

        keyboardController?.hide()
        focusManager.clearFocus()
        isManualBarcodeFocused = false
        viewModel.registerManualBarcode(inventoryId)
    }

    fun registerQuantityCapture() {
        keyboardController?.hide()
        focusManager.clearFocus()
        expandedMedida = false
        viewModel.registerCurrentScan(inventoryId)
    }

    if (enableScanner) {
        DisposableEffect(context, inventoryId, uiState.scanMode) {
            Log.d(TAG_SCAN_CAPTURE, "Registrando UnitechScan directo en captura")

            val scanner = UnitechScan(context) { code, codeId ->
                val barcode = normalizeBarcodeInput(code)

                Log.d(
                    TAG_SCAN_CAPTURE,
                    "UnitechScan recibido code='$barcode', codeId=$codeId, modo=${uiState.scanMode}"
                )

                if (isValidBarcodeValue(barcode)) {
                    if (uiState.scanMode == CaptureMode.UNIT) {
                        viewModel.registerDetectedBarcode(
                            inventoryId = inventoryId,
                            barcode = barcode
                        )
                    } else {
                        viewModel.onBarcodeDetected(barcode)
                        focusQuantityField()
                    }
                } else {
                    Log.d(TAG_SCAN_CAPTURE, "UnitechScan código inválido='$barcode'")
                }
            }

            scanner.register()

            onDispose {
                Log.d(TAG_SCAN_CAPTURE, "Desregistrando UnitechScan directo en captura")
                scanner.unregister()
            }
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.successMessageId) {
        val message = uiState.successMessage.orEmpty()

        if (message == "Producto registrado") {
            val modeAtSuccess = uiState.scanMode

            AppFloatingMessage.success(message)

            delay(PRODUCT_REGISTERED_MESSAGE_MS)

            viewModel.clearRegisteredProductInfo()

            if (modeAtSuccess == CaptureMode.QUANTITY) {
                focusBarcodeField()
            } else {
                restoreScannerFocus()
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        restoreScannerFocus()
    }

    LaunchedEffect(inventoryId) {
        viewModel.loadInventory(inventoryId)
        delay(300)
        restoreScannerFocus()
    }

    LaunchedEffect(uiState.scanMode) {
        delay(100)

        if (uiState.scanMode == CaptureMode.QUANTITY) {
            focusBarcodeField()
        } else {
            restoreScannerFocus()
        }
    }

    LaunchedEffect(
        uiState.units.size,
        uiState.ubicaciones.size,
        uiState.selectedUnitId,
        uiState.selectedUbicacionId
    ) {
        delay(200)

        if (uiState.scanMode == CaptureMode.UNIT) {
            restoreScannerFocus()
        }
    }

    LaunchedEffect(scannerBuffer) {
        if (scannerBuffer.isBlank()) return@LaunchedEffect

        Log.d(TAG_SCAN_CAPTURE, "Detectado scannerBuffer='$scannerBuffer'")

        val currentVersion = ++scanVersion
        delay(250)

        if (currentVersion == scanVersion && scannerBuffer.isNotBlank()) {
            val nuevoCodigo = normalizeBarcodeInput(scannerBuffer)

            Log.d(TAG_SCAN_CAPTURE, "Codigo normalizado por teclado='$nuevoCodigo'")

            if (isValidBarcodeValue(nuevoCodigo)) {
                Log.d(
                    TAG_SCAN_CAPTURE,
                    "Codigo teclado valido='$nuevoCodigo', modo=${uiState.scanMode}"
                )

                if (uiState.scanMode == CaptureMode.UNIT) {
                    viewModel.registerDetectedBarcode(
                        inventoryId = inventoryId,
                        barcode = nuevoCodigo
                    )
                } else {
                    viewModel.onBarcodeDetected(nuevoCodigo)
                    focusQuantityField()
                }
            } else {
                Log.d(TAG_SCAN_CAPTURE, "Codigo teclado invalido='$nuevoCodigo'")
            }

            scannerBuffer = ""

            if (uiState.scanMode == CaptureMode.UNIT) {
                restoreScannerFocus()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("capture_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = scannerBuffer,
                onValueChange = { value ->
                    Log.d(TAG_SCAN_CAPTURE, "scannerBuffer='$value'")
                    scannerBuffer = value
                },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .testTag("input_scanner_buffer")
                    .focusRequester(scannerFocusRequester),
                singleLine = true,
                label = { Text("") }
            )

            ExposedDropdownMenuBox(
                expanded = expandedUbicacion,
                onExpandedChange = { expandedUbicacion = !expandedUbicacion },
                modifier = Modifier.fillMaxWidth()
            ) {
                FloatingLabelContainer(
                    label = "Seleccione ubicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                ) {
                    OutlinedTextField(
                        value = uiState.selectedUbicacionName,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUbicacion)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("input_ubicacion"),
                        shape = RoundedCornerShape(16.dp),
                        colors = inventoryTextFieldColors()
                    )
                }

                DropdownMenu(
                    expanded = expandedUbicacion,
                    onDismissRequest = {
                        expandedUbicacion = false
                        restoreScannerFocus()
                    }
                ) {
                    uiState.ubicaciones.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.nombre) },
                            onClick = {
                                viewModel.onUbicacionSelected(item.id)
                                expandedUbicacion = false
                                restoreScannerFocus()
                            }
                        )
                    }
                }
            }

            FieldSpacer()

            Text(
                text = "Tipo captura",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabelSpacing))

            SlideCaptureSelector(
                selectedMode = uiState.scanMode,
                onSelectUnit = {
                    viewModel.onScanModeChanged(CaptureMode.UNIT)
                    restoreScannerFocus()
                },
                onSelectQuantity = {
                    viewModel.onScanModeChanged(CaptureMode.QUANTITY)
                    focusBarcodeField()
                }
            )

            FieldSpacer()

            BarcodeInventoryTextField(
                value = uiState.barcode,
                onValueChange = { value ->
                    viewModel.onManualBarcodeChanged(value)
                },
                label = "Código de barra",
                keyboardType = KeyboardType.Text,
                imeAction = if (uiState.scanMode == CaptureMode.UNIT) {
                    ImeAction.Done
                } else {
                    ImeAction.Next
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        registerManualBarcodeFromKeyboard()
                    },
                    onNext = {
                        focusQuantityField()
                    }
                ),
                minHeight = 56.dp,
                readOnly = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_barcode")
                    .focusRequester(barcodeFocusRequester)
                    .onFocusChanged { focusState ->
                        if (
                            isManualBarcodeFocused &&
                            !focusState.isFocused &&
                            uiState.scanMode == CaptureMode.UNIT
                        ) {
                            viewModel.lookupManualBarcodeInfo()
                        }

                        isManualBarcodeFocused = focusState.isFocused
                    }
            )

            if (uiState.scanMode == CaptureMode.UNIT) {
                FieldSpacer()

                AppPrimaryButton(
                    text = "Registrar",
                    icon = Icons.Default.CheckCircle,
                    enabled = uiState.barcode.trim().isNotBlank(),
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        isManualBarcodeFocused = false
                        viewModel.registerManualBarcode(inventoryId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_register_capture")
                )

                FieldSpacer()

                UnitDropdownField(
                    expanded = expandedMedida,
                    onExpandedChange = { expandedMedida = it },
                    selectedUnitName = uiState.selectedUnitName,
                    units = uiState.units,
                    onUnitSelected = { unitId ->
                        viewModel.onUnitSelected(unitId)
                        expandedMedida = false
                        restoreScannerFocus()
                    },
                    onDismiss = {
                        expandedMedida = false
                        restoreScannerFocus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_unit_measure")
                )
            } else {
                FieldSpacer()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FieldSpacing)
                ) {
                    InventoryTextField(
                        value = uiState.quantityInput,
                        onValueChange = { viewModel.onQuantityChanged(it) },
                        label = "Cantidad",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                registerQuantityCapture()
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_quantity")
                            .focusRequester(quantityFocusRequester),
                        minHeight = 56.dp
                    )

                    UnitDropdownField(
                        expanded = expandedMedida,
                        onExpandedChange = { expandedMedida = it },
                        selectedUnitName = uiState.selectedUnitName,
                        units = uiState.units,
                        onUnitSelected = { unitId ->
                            viewModel.onUnitSelected(unitId)
                            expandedMedida = false
                        },
                        onDismiss = {
                            expandedMedida = false
                        },
                        focusRequester = unitMeasureFocusRequester,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_unit_measure")
                    )
                }

                FieldSpacer()

                AppPrimaryButton(
                    text = "Registrar",
                    icon = Icons.Default.CheckCircle,
                    enabled = uiState.barcode.trim().isNotBlank() &&
                            uiState.quantityInput.trim().isNotBlank(),
                    onClick = {
                        registerQuantityCapture()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_register_capture")
                )
            }

            FieldSpacer()

            InventoryTextField(
                value = uiState.description,
                onValueChange = {},
                label = "Descripción",
                keyboardType = KeyboardType.Text,
                minHeight = 56.dp,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_description")
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(LabelSpacing))

                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BottomContentPadding),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                InventoryMenuButton(
                    text = "Ver",
                    icon = Icons.Default.ListAlt,
                    onClick = onViewList,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_view_inventory")
                )

                InventoryMenuButton(
                    text = "Pendiente",
                    icon = Icons.Default.PauseCircle,
                    onClick = onLeavePending,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_leave_pending")
                )

                InventoryMenuButton(
                    text = "Finalizar",
                    icon = Icons.Default.Inventory2,
                    onClick = {
                        viewModel.finalizeInventory(
                            inventoryId = inventoryId,
                            onFinished = onFinishInventory
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_finish_inventory")
                )
            }
        }

        AppFloatingMessageHost(
            durationMillis = PRODUCT_REGISTERED_MESSAGE_MS
        )
    }
}

@Composable
private fun FieldSpacer() {
    Spacer(modifier = Modifier.height(FieldSpacing))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdownField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedUnitName: String,
    units: List<UnidadMedidaOption>,
    onUnitSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) },
        modifier = modifier
    ) {
        FloatingLabelContainer(
            label = "Unidad medida",
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        ) {
            OutlinedTextField(
                value = selectedUnitName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = inventoryTextFieldColors()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            units.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.nombre) },
                    onClick = { onUnitSelected(item.id) }
                )
            }
        }
    }
}

private fun normalizeBarcodeInput(value: String): String {
    return value
        .filter { it.isLetterOrDigit() }
        .take(MAX_BARCODE_LENGTH)
}

private fun isValidBarcodeValue(value: String): Boolean {
    val code = normalizeBarcodeInput(value)

    if (code.isBlank()) return false
    if (code.length > MAX_BARCODE_LENGTH) return false

    return code.all { it.isLetterOrDigit() }
}

@Composable
private fun SlideCaptureSelector(
    selectedMode: CaptureMode,
    onSelectUnit: () -> Unit,
    onSelectQuantity: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("selector_capture_mode")
            .clip(RoundedCornerShape(24.dp))
            .background(White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .testTag("capture_mode_unit")
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selectedMode == CaptureMode.UNIT) {
                            Brush.horizontalGradient(listOf(BrandPrimary, BrandPrimaryDark))
                        } else {
                            Brush.horizontalGradient(listOf(White, White))
                        }
                    )
                    .clickable { onSelectUnit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Unidad",
                    color = if (selectedMode == CaptureMode.UNIT) White else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .testTag("capture_mode_quantity")
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selectedMode == CaptureMode.QUANTITY) {
                            Brush.horizontalGradient(listOf(BrandPrimary, BrandPrimaryDark))
                        } else {
                            Brush.horizontalGradient(listOf(White, White))
                        }
                    )
                    .clickable { onSelectQuantity() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cantidad",
                    color = if (selectedMode == CaptureMode.QUANTITY) White else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BarcodeInventoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minHeight: Dp = 64.dp,
    readOnly: Boolean = false
) {
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        } else if (fieldValue.selection.end != value.length) {
            fieldValue = fieldValue.copy(
                selection = TextRange(value.length)
            )
        }
    }

    FloatingLabelContainer(
        label = label,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val clean = normalizeBarcodeInput(newValue.text)

                fieldValue = TextFieldValue(
                    text = clean,
                    selection = TextRange(clean.length)
                )

                onValueChange(clean)
            },
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            shape = RoundedCornerShape(16.dp),
            colors = inventoryTextFieldColors()
        )
    }
}

@Composable
private fun InventoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minHeight: Dp = 64.dp,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    FloatingLabelContainer(
        label = label,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = true,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            shape = RoundedCornerShape(16.dp),
            colors = inventoryTextFieldColors()
        )
    }
}

@Composable
private fun FloatingLabelContainer(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LabelTopPadding)
        ) {
            content()
        }

        Text(
            text = label,
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 16.dp)
                .background(Background)
                .padding(horizontal = 7.dp)
                .zIndex(1f)
        )
    }
}

