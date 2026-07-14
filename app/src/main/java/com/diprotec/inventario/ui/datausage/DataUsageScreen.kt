package com.diprotec.inventario.ui.datausage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventario.data.local.dao.NetworkUsageGroupRow
import com.diprotec.inventario.ui.common.AppFloatingMessage
import com.diprotec.inventario.ui.components.AppCard
import com.diprotec.inventario.ui.components.AppPrimaryButton
import com.diprotec.inventario.ui.components.SectionTitle
import com.diprotec.inventario.ui.theme.Background
import com.diprotec.inventario.ui.theme.BorderGray
import com.diprotec.inventario.ui.theme.Dimens
import com.diprotec.inventario.ui.theme.InventoryMenuButton
import com.diprotec.inventario.ui.theme.TextPrimary

@Composable
fun DataUsageScreen(
    onBack: () -> Unit,
    viewModel: DataUsageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            AppFloatingMessage.info(it)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Dimens.spaceXl)
    ) {
        Text(
            text = "Consumo de datos",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.padding(Dimens.spaceXs))

        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.padding(Dimens.spaceXs))

                Text("Cargando consumo…")
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)
                ) {
                    SummaryCard(
                        title = "Hoy",
                        value = state.today,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Últimos 7 días",
                        value = state.last7Days,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)
                ) {
                    SummaryCard(
                        title = "Llamadas hoy",
                        value = state.todayCalls.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Promedio llamada",
                        value = state.averagePerCall,
                        modifier = Modifier.weight(1f)
                    )
                }

                UsageGroupCard(
                    title = "Consumo por origen",
                    rows = state.bySource,
                    formatBytes = viewModel::formatBytes
                )

                UsageGroupCard(
                    title = "Consumo por operación",
                    rows = state.byOperation,
                    formatBytes = viewModel::formatBytes
                )

                UsageGroupCard(
                    title = "Consumo por endpoint",
                    rows = state.byEndpoint,
                    formatBytes = viewModel::formatBytes
                )
            }
        }

        Spacer(modifier = Modifier.padding(Dimens.spaceS))

        AppPrimaryButton(
            text = "Actualizar",
            icon = Icons.Default.Refresh,
            iconContentDescription = null,
            onClick = viewModel::refresh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(Dimens.spaceXxs))

        AppPrimaryButton(
            text = "Limpiar registros",
            icon = Icons.Default.Delete,
            iconContentDescription = null,
            onClick = viewModel::clearLogs,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(Dimens.spaceS))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            InventoryMenuButton(
                text = "Volver",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
    ) {
        Column {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.padding(Dimens.spaceXxs))

            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UsageGroupCard(
    title: String,
    rows: List<NetworkUsageGroupRow>,
    formatBytes: (Long) -> String
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column {
            SectionTitle(text = title)

            Spacer(modifier = Modifier.padding(Dimens.spaceXs))

            if (rows.isEmpty()) {
                Text(
                    text = "Sin registros",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                rows.forEachIndexed { index, row ->
                    UsageRow(
                        row = row,
                        formatBytes = formatBytes
                    )

                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Dimens.spaceS),
                            color = BorderGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageRow(
    row: NetworkUsageGroupRow,
    formatBytes: (Long) -> String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = row.name,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.padding(2.dp))

        Text(
            text = "${formatBytes(row.totalBytes)} · ${row.callCount} llamadas",
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
