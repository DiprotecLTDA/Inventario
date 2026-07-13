package com.diprotec.inventario.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.config.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StartupUpdateUiState(
    val showDialog: Boolean = false,
    val mandatory: Boolean = false,
    val downloadId: Long = -1L,
    val apkFileName: String = "",
    val apkUrl: String = "",
    val apkDownloaded: Boolean = false
)

@HiltViewModel
class StartupUpdateViewModel @Inject constructor(
    private val settings: SettingsManager
) : ViewModel() {

    val state = combine(
        settings.pendingUpdateMandatory,
        settings.pendingDownloadId,
        settings.pendingApkFileName,
        settings.pendingApkUrl,
        settings.apkDownloaded
    ) { mandatory, downloadId, apkFileName, apkUrl, apkDownloaded ->
        StartupUpdateUiState(
            showDialog = apkUrl.isNotBlank() && apkFileName.isNotBlank(),
            mandatory = mandatory,
            downloadId = downloadId,
            apkFileName = apkFileName,
            apkUrl = apkUrl,
            apkDownloaded = apkDownloaded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StartupUpdateUiState()
    )

    fun dismissOptionalUpdate() {
        viewModelScope.launch {
            settings.clearPendingUpdate()
        }
    }

    fun saveDownloadState(
        mandatory: Boolean,
        downloadId: Long,
        apkFileName: String,
        apkUrl: String,
        apkDownloaded: Boolean
    ) {
        viewModelScope.launch {
            settings.savePendingUpdate(
                mandatory = mandatory,
                downloadId = downloadId,
                apkFileName = apkFileName,
                apkUrl = apkUrl,
                apkDownloaded = apkDownloaded
            )
        }
    }

    fun markApkDownloaded(downloadId: Long) {
        viewModelScope.launch {
            settings.updatePendingDownloadState(
                downloadId = downloadId,
                apkDownloaded = true
            )
        }
    }

    fun resetPendingUpdate() {
        viewModelScope.launch {
            settings.clearPendingUpdate()
        }
    }
}