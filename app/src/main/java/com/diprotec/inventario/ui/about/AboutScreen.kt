package com.diprotec.inventario.ui.about

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.components.AppPrimaryButton
import com.diprotec.inventario.ui.components.InventoryTopBar
import com.diprotec.inventario.ui.components.OutlinedInfoCard
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.Dimens
import com.diprotec.inventario.ui.theme.TextPrimary
import com.diprotec.inventario.ui.theme.White
import com.diprotec.inventario.ui.update.StartupUpdateDialog
import java.util.Locale

@Composable
fun AboutScreen(
    onBack: () -> Unit = {},
    vm: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val packageName = context.packageName

    val packageInfo = remember {
        pm.getPackageInfo(packageName, 0)
    }

    val appInfo = remember {
        pm.getApplicationInfo(packageName, 0)
    }

    val appName = remember {
        pm.getApplicationLabel(appInfo).toString()
    }

    val versionName = remember {
        packageInfo.versionName ?: "-"
    }

    val appIconBitmap = remember {
        pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
    }

    val model = remember {
        Build.MODEL.orEmpty()
    }

    val s by vm.state
    val remoteVersion = s.versionCheck?.version
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        vm.loadRemoteVersion()
    }

    LaunchedEffect(s.successMessage) {
        s.successMessage?.let {
            AppFloatingMessage.info(it)
            vm.clearMessages()
        }
    }

    LaunchedEffect(s.errorMessage) {
        s.errorMessage?.let {
            AppFloatingMessage.error(it)
            vm.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        InventoryTopBar(title = "ACERCA DE")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.spaceXl, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = White,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = appName,
                        modifier = Modifier.size(88.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            AppPrimaryButton(
                text = when {
                    s.loading -> "Consultando..."
                    !s.canCheckUpdates -> "Sin conexión"
                    else -> "Chequear versión"
                },
                onClick = {
                    vm.loadRemoteVersion()
                },
                enabled = !s.loading && s.canCheckUpdates,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AboutRow(
                    label = "Versión instalada",
                    value = versionName
                )

                AboutRow(
                    label = "Tamaño actualización",
                    value = formatBytes(remoteVersion?.fileSizeBytes)
                )

                AboutRow(
                    label = "Modelo",
                    value = model.ifBlank { "-" }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spaceXxl))
        }
    }

    StartupUpdateDialog()
}

private fun formatBytes(
    bytesText: String?
): String {
    val bytes = bytesText?.trim()?.toLongOrNull() ?: return "-"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(Dimens.spaceXxs))

        OutlinedInfoCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            contentPadding = PaddingValues(horizontal = Dimens.spaceL, vertical = 14.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}
