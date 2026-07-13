package com.diprotec.inventario.ui.menu

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.diprotec.inventario.core.network.AppConnectionMonitor
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.ui.connection.AppConnectionMode
import com.diprotec.inventario.worker.PendingInventorySyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PendingSyncUiState(
    val syncing: Boolean = false,
    val message: String? = null,
    val connectionMode: AppConnectionMode = AppConnectionMode.CHECKING,
    val canSyncPending: Boolean = false
)

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val monitor: AppConnectionMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentUsername = sessionManager.loginRut

    private val _pendingSyncState = mutableStateOf(PendingSyncUiState())
    val pendingSyncState: State<PendingSyncUiState> = _pendingSyncState

    private val _sessionRemainingText = mutableStateOf(
        formatMillis(sessionManager.maxSessionMs)
    )
    val sessionRemainingText: State<String> = _sessionRemainingText

    private var activeLiveData: LiveData<WorkInfo>? = null
    private var activeObserver: Observer<WorkInfo>? = null
    private var sessionExpiredHandled = false

    init {
        viewModelScope.launch {
            monitor.observeMode().collect { mode ->
                _pendingSyncState.value = _pendingSyncState.value.copy(
                    connectionMode = mode,
                    canSyncPending = mode == AppConnectionMode.ONLINE_API &&
                            !_pendingSyncState.value.syncing
                )
            }
        }

        viewModelScope.launch {
            while (true) {
                updateSessionRemainingText()
                delay(1_000L)
            }
        }
    }

    fun syncPendingInventories() {
        if (_pendingSyncState.value.syncing) return

        if (_pendingSyncState.value.connectionMode != AppConnectionMode.ONLINE_API) {
            _pendingSyncState.value = _pendingSyncState.value.copy(
                syncing = false,
                canSyncPending = false,
                message = "Sin conexión a internet. No se puede sincronizar pendientes."
            )
            return
        }

        clearActiveObserver()

        _pendingSyncState.value = _pendingSyncState.value.copy(
            syncing = true,
            canSyncPending = false,
            message = null
        )

        val workId = PendingInventorySyncWorker.runOnce(context)
        val workManager = WorkManager.getInstance(context)
        val liveData = workManager.getWorkInfoByIdLiveData(workId)

        val observer = Observer<WorkInfo> { info ->
            val state = info?.state ?: return@Observer

            when {
                state == WorkInfo.State.SUCCEEDED -> {
                    clearActiveObserver()
                    _pendingSyncState.value = _pendingSyncState.value.copy(
                        syncing = false,
                        canSyncPending = _pendingSyncState.value.connectionMode == AppConnectionMode.ONLINE_API,
                        message = "Sincronización de pendientes finalizada"
                    )
                }

                state == WorkInfo.State.FAILED -> {
                    clearActiveObserver()
                    _pendingSyncState.value = _pendingSyncState.value.copy(
                        syncing = false,
                        canSyncPending = _pendingSyncState.value.connectionMode == AppConnectionMode.ONLINE_API,
                        message = "No se pudo sincronizar pendientes"
                    )
                }

                state == WorkInfo.State.CANCELLED -> {
                    clearActiveObserver()
                    _pendingSyncState.value = _pendingSyncState.value.copy(
                        syncing = false,
                        canSyncPending = _pendingSyncState.value.connectionMode == AppConnectionMode.ONLINE_API,
                        message = "Sincronización cancelada"
                    )
                }

                state == WorkInfo.State.ENQUEUED && info.runAttemptCount > 0 -> {
                    clearActiveObserver()
                    _pendingSyncState.value = _pendingSyncState.value.copy(
                        syncing = false,
                        canSyncPending = _pendingSyncState.value.connectionMode == AppConnectionMode.ONLINE_API,
                        message = "No se pudo sincronizar ahora. Se reintentará automáticamente."
                    )
                }
            }
        }

        activeLiveData = liveData
        activeObserver = observer
        liveData.observeForever(observer)
    }

    fun clearPendingSyncMessage() {
        _pendingSyncState.value = _pendingSyncState.value.copy(
            message = null
        )
    }

    private fun updateSessionRemainingText() {
        val lastActivityAt: Long = sessionManager.sessionLastActivityAt.value
        val maxSessionMs: Long = sessionManager.maxSessionMs

        if (lastActivityAt <= 0L) {
            _sessionRemainingText.value = formatMillis(maxSessionMs)
            return
        }

        val now: Long = System.currentTimeMillis()
        val elapsedMs: Long = now - lastActivityAt
        val remainingMs: Long = maxSessionMs - elapsedMs

        _sessionRemainingText.value = formatMillis(remainingMs)

        if (remainingMs <= 0L && !sessionExpiredHandled) {
            sessionExpiredHandled = true

            viewModelScope.launch {
                sessionManager.logout()
            }
        }
    }

    private fun formatMillis(valueMs: Long): String {
        val totalSeconds: Long = (valueMs / 1_000L).coerceAtLeast(0L)

        val hours: Long = totalSeconds / 3_600L
        val minutes: Long = (totalSeconds % 3_600L) / 60L
        val seconds: Long = totalSeconds % 60L

        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    private fun clearActiveObserver() {
        val liveData = activeLiveData
        val observer = activeObserver

        if (liveData != null && observer != null) {
            liveData.removeObserver(observer)
        }

        activeLiveData = null
        activeObserver = null
    }

    override fun onCleared() {
        clearActiveObserver()
        super.onCleared()
    }
}