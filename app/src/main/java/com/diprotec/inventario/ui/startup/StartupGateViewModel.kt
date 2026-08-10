package com.diprotec.inventario.ui.startup

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.data.local.dao.UserDao
import com.diprotec.inventario.service.SyncService
import com.diprotec.inventario.worker.CatalogSyncWorker
import com.diprotec.inventario.worker.PendingInventorySyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StartupGateUiState(
    val loading: Boolean = true,
    val message: String = AppMessages.Startup.VERIFICANDO_APLICACION,
    val error: String? = null,
    val waitingForUpdate: Boolean = false,
    val goLogin: Boolean = false,
    val goMainMenu: Boolean = false,
    val goSettings: Boolean = false,
    val canContinueOffline: Boolean = false
)

@HiltViewModel
class StartupGateViewModel @Inject constructor(
    private val settings: SettingsManager,
    private val session: SessionManager,
    private val sync: SyncService,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = mutableStateOf(StartupGateUiState())
    val state: State<StartupGateUiState> = _state

    private var started = false
    private var continuingAfterOptional = false

    fun start() {
        if (started) return
        started = true

        val hasBase = settings.baseUrl.value.trim().isNotBlank()
        val hasEmpresa = settings.empresaRut.value.trim().isNotBlank()
        val hasApiKey = settings.apiKey.value.trim().isNotBlank()
        val hasAuth = settings.authToken.value.trim().isNotBlank()

        if (!hasBase || !hasEmpresa || !hasApiKey || !hasAuth) {
            _state.value = _state.value.copy(
                loading = false,
                message = AppMessages.Configuration.FALTAN_PARAMETROS,
                error = AppMessages.Configuration.FALTAN_PARAMETROS_DETALLE,
                goSettings = true,
                canContinueOffline = false
            )
            return
        }

        if (!settings.deviceActivated.value) {
            _state.value = _state.value.copy(
                loading = false,
                message = AppMessages.Device.NO_ACTIVADO_CORTO,
                error = AppMessages.Device.NO_ACTIVADO_CONFIGURACION,
                goSettings = true,
                canContinueOffline = false
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                message = AppMessages.Startup.PREPARANDO_SESION,
                error = null,
                waitingForUpdate = false,
                goLogin = false,
                goMainMenu = false,
                goSettings = false,
                canContinueOffline = false
            )

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    sync.warmUpDeviceSession()

                    _state.value = _state.value.copy(
                        message = AppMessages.Startup.CONSULTANDO_ACTUALIZACION
                    )

                    sync.checkStartupUpdateAndSavePending()
                }
            }

            result.onFailure { t ->
                Log.e(TAG, "Startup gate failed", t)

                if (canWorkOffline()) {
                    showOfflineOption(
                        message = AppMessages.Startup.SIN_CONEXION,
                        error = AppMessages.Startup.NO_SE_PUDO_VERIFICAR_OFFLINE
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = AppMessages.Startup.NO_SE_PUDO_VERIFICAR,
                        error = t.message ?: AppMessages.Startup.NO_SE_PUDO_VERIFICAR,
                        goLogin = false,
                        goMainMenu = false,
                        goSettings = false,
                        waitingForUpdate = false,
                        canContinueOffline = false
                    )
                }
            }

            result.onSuccess { update ->
                val hasUpdate =
                    update.hasNewVersion &&
                            !update.apkUrl.isNullOrBlank() &&
                            update.apkFileName.isNotBlank()

                if (hasUpdate) {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = if (update.mandatory) {
                            AppMessages.Startup.ACTUALIZACION_OBLIGATORIA
                        } else {
                            AppMessages.Startup.NUEVA_VERSION
                        },
                        error = null,
                        waitingForUpdate = true,
                        goLogin = false,
                        goMainMenu = false,
                        goSettings = false,
                        canContinueOffline = false
                    )
                } else {
                    continueToLogin()
                }
            }
        }
    }

    fun retry() {
        started = false
        continuingAfterOptional = false
        start()
    }

    fun continueOffline() {
        viewModelScope.launch {
            goLoginOffline()
        }
    }

    fun continueAfterOptionalUpdateDismissed() {
        if (continuingAfterOptional) return
        continuingAfterOptional = true

        viewModelScope.launch {
            continueToLogin()
            continuingAfterOptional = false
        }
    }

    fun onUpdateStarted() {
        _state.value = _state.value.copy(
            loading = false,
            waitingForUpdate = true,
            goLogin = false,
            goMainMenu = false,
            goSettings = false,
            canContinueOffline = false,
            message = AppMessages.Startup.DESCARGANDO_ACTUALIZACION
        )
    }

    private suspend fun continueToLogin() {
        _state.value = _state.value.copy(
            loading = true,
            message = AppMessages.Startup.SINCRONIZANDO_USUARIOS,
            error = null,
            waitingForUpdate = false,
            goLogin = false,
            goMainMenu = false,
            goSettings = false,
            canContinueOffline = false
        )

        val result = withContext(Dispatchers.IO) {
            runCatching {
                sync.syncUsers()
            }
        }

        result.onFailure { t ->
            Log.e(TAG, "syncUsers failed", t)

            if (canWorkOffline()) {
                showOfflineOption(
                    message = AppMessages.Startup.SIN_CONEXION,
                    error = AppMessages.Startup.NO_SE_PUDO_SINCRONIZAR_USUARIOS_OFFLINE
                )
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    message = AppMessages.Startup.NO_SE_PUDO_SINCRONIZAR_USUARIOS,
                    error = t.message ?: AppMessages.Startup.NO_SE_PUDO_SINCRONIZAR_USUARIOS,
                    goLogin = false,
                    goMainMenu = false,
                    goSettings = false,
                    waitingForUpdate = false,
                    canContinueOffline = false
                )
            }
        }

        result.onSuccess {
            CatalogSyncWorker.schedulePeriodic(context)
            PendingInventorySyncWorker.schedulePeriodic(context)

            goToSessionDestination()
        }
    }

    private suspend fun canWorkOffline(): Boolean = withContext(Dispatchers.IO) {
        // Solo se exige que existan usuarios sincronizados (o de una sesión previa).
        // Catálogos vacíos (reglas/ubicaciones/productos/unidades) no bloquean el ingreso
        // offline: se sincronizan best-effort cuando vuelva la red.
        userDao.countUsers() > 0
    }

    private fun showOfflineOption(
        message: String,
        error: String
    ) {
        _state.value = _state.value.copy(
            loading = false,
            message = message,
            error = error,
            waitingForUpdate = false,
            goLogin = false,
            goMainMenu = false,
            goSettings = false,
            canContinueOffline = true
        )
    }

    private suspend fun goLoginOffline() {
        CatalogSyncWorker.schedulePeriodic(context)
        PendingInventorySyncWorker.schedulePeriodic(context)

        goToSessionDestination(
            offline = true
        )
    }

    private suspend fun goToSessionDestination(
        offline: Boolean = false
    ) {
        val hasValidSession = session.restoreSession()

        _state.value = _state.value.copy(
            loading = false,
            message = if (offline) {
                AppMessages.Startup.MODO_OFFLINE
            } else {
                AppMessages.Startup.APLICACION_LISTA
            },
            error = null,
            waitingForUpdate = false,
            goLogin = !hasValidSession,
            goMainMenu = hasValidSession,
            goSettings = false,
            canContinueOffline = false
        )
    }

    companion object {
        private const val TAG = "STARTUP_GATE"
    }
}
