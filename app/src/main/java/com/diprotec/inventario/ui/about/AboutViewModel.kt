package com.diprotec.inventario.ui.about

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.network.AppConnectionMonitor
import com.diprotec.inventario.data.remote.dto.VersionCheckDataDto
import com.diprotec.inventario.service.VersionService
import com.diprotec.inventario.ui.connection.AppConnectionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class AboutUiState(
    val loading: Boolean = false,
    val versionCheck: VersionCheckDataDto? = null,
    val error: String? = null,
    val info: String? = null,
    val hasNewVersion: Boolean = false,
    val isMandatoryUpdate: Boolean = false,
    val canOperate: Boolean = true,
    val connectionMode: AppConnectionMode = AppConnectionMode.CHECKING,
    val canCheckUpdates: Boolean = false
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val versionService: VersionService,
    private val settingsManager: SettingsManager,
    private val monitor: AppConnectionMonitor
) : ViewModel() {

    var state = mutableStateOf(AboutUiState())
        private set

    init {
        viewModelScope.launch {
            monitor.observeMode().collect { mode ->
                state.value = state.value.copy(
                    connectionMode = mode,
                    canCheckUpdates = mode == AppConnectionMode.ONLINE_API
                )
            }
        }
    }

    fun getCurrentVersionCode(): String =
        versionService.getCurrentVersionCode()

    fun getCurrentVersionName(): String =
        versionService.getCurrentVersionName()

    fun getAndroidVersionName(): String =
        Build.VERSION.RELEASE.orEmpty().trim().ifBlank { "-" }

    fun getAndroidSdk(): String =
        Build.VERSION.SDK_INT.toString()

    fun loadRemoteVersion() {
        if (state.value.loading) return

        state.value = state.value.copy(
            loading = true,
            error = null,
            info = null
        )

        viewModelScope.launch {
            val mode = monitor.checkMode()

            if (mode != AppConnectionMode.ONLINE_API) {
                state.value = state.value.copy(
                    loading = false,
                    connectionMode = mode,
                    canCheckUpdates = false,
                    versionCheck = null,
                    error = AppMessages.About.SIN_CONEXION_CONSULTAR_VERSION,
                    info = null,
                    hasNewVersion = false,
                    isMandatoryUpdate = false,
                    canOperate = true
                )
                return@launch
            }

            runCatching { versionService.checkVersion() }
                .onSuccess { data ->
                    val version = data?.version

                    val apiRequiresUpdate =
                        data?.requiereActualizacionBool() == true

                    val hasNewVersion =
                        versionService.hasNewVersion(data) || apiRequiresUpdate

                    val isMandatoryUpdate =
                        data?.actualizacionObligatoriaBool() == true

                    val canOperate =
                        versionService.canOperate(data)

                    val updateUrl = if (hasNewVersion) {
                        versionService.buildApkUrl(version)
                    } else {
                        null
                    }

                    val apkFileName = version?.apkFileName?.trim().orEmpty()

                    if (hasNewVersion && !updateUrl.isNullOrBlank() && apkFileName.isNotBlank()) {
                        settingsManager.savePendingUpdate(
                            mandatory = isMandatoryUpdate,
                            downloadId = -1L,
                            apkFileName = apkFileName,
                            apkUrl = updateUrl,
                            apkDownloaded = false
                        )
                    } else {
                        settingsManager.clearPendingUpdate()
                    }

                    state.value = state.value.copy(
                        loading = false,
                        versionCheck = data,
                        error = null,
                        info = when {
                            hasNewVersion && isMandatoryUpdate -> {
                                AppMessages.About.ACTUALIZACION_OBLIGATORIA_ENCONTRADA
                            }

                            hasNewVersion -> {
                                AppMessages.About.ACTUALIZACION_OPCIONAL_ENCONTRADA
                            }

                            else -> {
                                AppMessages.About.APLICACION_ACTUALIZADA
                            }
                        },
                        hasNewVersion = hasNewVersion,
                        isMandatoryUpdate = isMandatoryUpdate,
                        canOperate = canOperate,
                        connectionMode = AppConnectionMode.ONLINE_API,
                        canCheckUpdates = true
                    )
                }
                .onFailure { t ->
                    state.value = state.value.copy(
                        loading = false,
                        versionCheck = null,
                        error = t.message ?: AppMessages.About.NO_SE_PUDO_CONSULTAR_VERSION,
                        info = null,
                        hasNewVersion = false,
                        isMandatoryUpdate = false,
                        canOperate = true
                    )
                }
        }
    }

    fun clearInfo() {
        state.value = state.value.copy(info = null)
    }

    fun clearError() {
        state.value = state.value.copy(error = null)
    }
}
