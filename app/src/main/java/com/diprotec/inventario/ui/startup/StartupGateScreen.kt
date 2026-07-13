package com.diprotec.inventario.ui.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.R
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.update.StartupUpdateDialog

@Composable
fun StartupGateScreen(
    onGoLogin: () -> Unit,
    onGoMainMenu: () -> Unit,
    onGoSettings: () -> Unit,
    vm: StartupGateViewModel = hiltViewModel()
) {
    val s by vm.state

    LaunchedEffect(Unit) {
        vm.start()
    }

    LaunchedEffect(s.error) {
        s.error?.let {
            AppFloatingMessage.error(it)
        }
    }

    LaunchedEffect(s.goLogin) {
        if (s.goLogin) {
            onGoLogin()
        }
    }

    LaunchedEffect(s.goMainMenu) {
        if (s.goMainMenu) {
            onGoMainMenu()
        }
    }

    LaunchedEffect(s.goSettings) {
        if (s.goSettings) {
            onGoSettings()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        StartupGateContent(
            loading = s.loading,
            message = s.message,
            error = s.error,
            canContinueOffline = s.canContinueOffline,
            onRetry = {
                vm.retry()
            },
            onContinueOffline = {
                vm.continueOffline()
            }
        )

        if (s.waitingForUpdate) {
            StartupUpdateDialog(
                onOptionalDismissed = {
                    vm.continueAfterOptionalUpdateDismissed()
                },
                onUpdateStarted = {
                    vm.onUpdateStarted()
                }
            )
        }
    }
}

@Composable
private fun StartupGateContent(
    loading: Boolean,
    message: String,
    error: String?,
    canContinueOffline: Boolean,
    onRetry: () -> Unit,
    onContinueOffline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_diprotec),
            contentDescription = "Diprotec",
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (loading) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (canContinueOffline) {
                    TextButton(
                        onClick = onContinueOffline
                    ) {
                        Text("Continuar offline")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                TextButton(
                    onClick = onRetry
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}