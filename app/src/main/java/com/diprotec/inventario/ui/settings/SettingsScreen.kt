package com.diprotec.inventario.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.theme.AppPrimaryButton
import com.diprotec.inventario.ui.theme.InventoryMenuButton
import com.diprotec.inventario.ui.theme.InventoryTopBar
import com.diprotec.inventario.ui.theme.LabelGray
import com.diprotec.inventario.ui.theme.SuccessBg
import com.diprotec.inventario.ui.theme.SuccessBorder
import com.diprotec.inventario.ui.theme.TextPrimary
import com.diprotec.inventario.ui.theme.inventoryTextFieldColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onDone: () -> Unit,
    onBack: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val s by vm.state
    val hasCreds = vm.hasCredentials()
    val deviceActivated = vm.isDeviceActivated()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var picking by remember { mutableStateOf(false) }

    if (s.errorMessage != null) {
        ErrorDialog(
            message = s.errorMessage ?: "",
            onDismiss = { vm.clearMessages() }
        )
    }

    val pickKeyFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        picking = false

        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = res.data?.data ?: return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        scope.launch {
            val ok = vm.importKeyFromUri(uri.toString())
            if (ok) {
                runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                }
            }
        }
    }

    fun launchPicker() {
        if (picking) return
        picking = true

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        pickKeyFile.launch(intent)
    }

    LaunchedEffect(s.successMessage) {
        s.successMessage?.let {
            AppFloatingMessage.info(it)
            vm.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            InventoryTopBar(title = "CONFIGURACIÓN")
        },
        bottomBar = {
            SettingsBottomActions(
                saving = s.saving,
                onPickCredentials = { launchPicker() },
                onSave = { vm.onSave(onDone) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsStatusHeader(
                hasCreds = hasCreds,
                deviceActivated = deviceActivated
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsTextField(
                    value = s.baseUrl,
                    onValueChange = vm::onBaseUrlChange,
                    label = "Base URL (termina con /)",
                    testTag = "input_base_url"
                )

                SettingsTextField(
                    value = s.empresaRut,
                    onValueChange = vm::onEmpresaChange,
                    label = "Empresa RUT",
                    keyboardType = KeyboardType.Ascii,
                    enabled = !deviceActivated,
                    testTag = "input_empresa_rut"
                )

                SettingsTextField(
                    value = s.activationCode,
                    onValueChange = vm::onActivationCodeChange,
                    label = "Código de activación",
                    keyboardType = KeyboardType.Text,
                    enabled = !deviceActivated,
                    testTag = "input_activation_code"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsStatusHeader(
    hasCreds: Boolean,
    deviceActivated: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Estado del dispositivo",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (hasCreds) {
            StatusBanner(
                text = "Credenciales cargadas",
                isSuccess = true
            )
        } else {
            NeutralStatus(
                text = "Aún no se han cargado credenciales."
            )
        }

        if (deviceActivated) {
            StatusBanner(
                text = "Dispositivo activado",
                isSuccess = true
            )
        } else {
            NeutralStatus(
                text = "El dispositivo aún no ha sido activado."
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Error de configuración",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Cerrar",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.45f)
            )
        }
    )
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = inventoryTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag(testTag)
    )
}

@Composable
private fun StatusBanner(
    text: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SuccessBg, RoundedCornerShape(12.dp))
            .border(1.dp, SuccessBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NeutralStatus(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = LabelGray,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = LabelGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettingsBottomActions(
    saving: Boolean,
    onPickCredentials: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            InventoryMenuButton(
                text = "Credenciales",
                icon = Icons.Default.Key,
                enabled = !saving,
                onClick = onPickCredentials,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_credenciales")
            )

            InventoryMenuButton(
                text = "Guardar",
                icon = Icons.Default.Save,
                enabled = !saving,
                loading = saving,
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_guardar")
            )
        }
    }
}
