package com.diprotec.inventario.ui.login

import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.core.format.RutFormat
import com.diprotec.inventario.core.format.RutInput
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.theme.LoginDesignScreen

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoSettings: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var picking by remember { mutableStateOf(false) }
    val s by vm.state
    val rutOk = s.username == "1" ||
            RutFormat.isComplete(RutInput.formatForDisplay(s.username))

    BackHandler {
        activity?.finishAndRemoveTask()
    }

    LaunchedEffect(Unit) {
        vm.warmUp()
    }

    LaunchedEffect(s.goToSettings) {
        if (s.goToSettings) {
            vm.clearGoToSettings()
            onGoSettings()
        }
    }

    LaunchedEffect(s.error) {
        s.error?.let {
            AppFloatingMessage.error(it)
            vm.clearError()
        }
    }

    LaunchedEffect(s.info) {
        s.info?.let {
            AppFloatingMessage.info(it)
            vm.clearInfo()
        }
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
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        vm.onKeyFileSelected(uri.toString())

        runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
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

    LaunchedEffect(s.needsPickKeyFile) {
        if (s.needsPickKeyFile) {
            launchPicker()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (s.loadingBoot) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Cargando…")

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Preparando inicio de sesión",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LoginDesignScreen(
                state = s,
                rutOk = rutOk,
                onUserChange = vm::onUserChange,
                onUserFocusLost = vm::onUserFocusLost,
                onPassChange = vm::onPassChange,
                onLoginClick = { vm.onLoginClick(onLoggedIn) },
                onSyncClick = vm::onSyncClick,
                onSettingsClick = onGoSettings,
                onPickFileClick = { launchPicker() }
            )
        }
    }
}