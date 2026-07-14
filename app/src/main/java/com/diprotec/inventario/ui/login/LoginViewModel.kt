package com.diprotec.inventario.ui.login

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.format.RutInput
import com.diprotec.inventario.core.key.KeyFileReader
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.core.validator.RutValidator
import com.diprotec.inventario.service.AuthResult
import com.diprotec.inventario.service.AuthService
import com.diprotec.inventario.service.InventarioSyncSummary
import com.diprotec.inventario.service.SyncService
import com.diprotec.inventario.worker.CatalogSyncWorker
import com.diprotec.inventario.worker.PendingInventorySyncWorker
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loadingBoot: Boolean = true,
    val loadingLogin: Boolean = false,
    val loadingSync: Boolean = false,
    val loadingSend: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val needsPickKeyFile: Boolean = false,
    val goToSettings: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authLazy: Lazy<AuthService>,
    private val syncLazy: Lazy<SyncService>,
    private val session: SessionManager,
    private val settings: SettingsManager,
    @ApplicationContext
    private val ctx: Context
) : ViewModel() {

    var state = mutableStateOf(LoginUiState())
        private set

    fun warmUp() {
        if (!state.value.loadingBoot) return

        val hasBase = settings.baseUrl.value.trim().isNotBlank()
        val hasEmpresa = settings.empresaRut.value.trim().isNotBlank()
        val hasApiKey = settings.apiKey.value.trim().isNotBlank()
        val hasAuth = settings.authToken.value.trim().isNotBlank()

        if (!hasBase || !hasEmpresa || !hasApiKey || !hasAuth) {
            state.value = state.value.copy(
                loadingBoot = false,
                errorMessage = AppMessages.Configuration.FALTAN_PARAMETROS_DETALLE,
                goToSettings = true
            )
            return
        }

        if (!settings.deviceActivated.value) {
            state.value = state.value.copy(
                loadingBoot = false,
                errorMessage = AppMessages.Device.NO_ACTIVADO_CONFIGURACION,
                goToSettings = true
            )
            return
        }

        state.value = state.value.copy(
            loadingBoot = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onUserChange(v: String) {
        val clean = RutInput.sanitizeForTyping(v)

        state.value = state.value.copy(
            username = clean,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onPassChange(v: String) {
        state.value = state.value.copy(
            password = v,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onLoginClick(onLoggedIn: () -> Unit) {
        val s = state.value

        val normalized = RutValidator.validateAndNormalize(s.username)
        if (normalized == null) {
            state.value = s.copy(
                username = "",
                loadingLogin = false,
                errorMessage = AppMessages.Login.RUT_INVALIDO
            )
            return
        }

        if (s.password.isBlank()) {
            state.value = s.copy(
                loadingLogin = false,
                errorMessage = AppMessages.Login.INGRESE_CONTRASENA
            )
            return
        }

        if (!settings.deviceActivated.value) {
            state.value = s.copy(
                loadingLogin = false,
                errorMessage = AppMessages.Device.NO_ACTIVADO_CONFIGURACION,
                goToSettings = true
            )
            return
        }

        state.value = s.copy(
            loadingLogin = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { syncLazy.get().warmUpDeviceSession() }
                    .onFailure { it.printStackTrace() }
            }

            val loginResult = withContext(Dispatchers.IO) {
                authLazy.get().login(normalized, s.password)
            }

            when (loginResult) {
                is AuthResult.Success -> {
                    session.login(
                        rut = normalized,
                        perfilId = loginResult.perfilId
                    )

                    CatalogSyncWorker.schedulePeriodic(ctx)
                    PendingInventorySyncWorker.schedulePeriodic(ctx)

                    CatalogSyncWorker.runOnce(ctx)
                    PendingInventorySyncWorker.runOnce(ctx)

                    state.value = state.value.copy(
                        loadingLogin = false,
                        errorMessage = null,
                        successMessage = AppMessages.Login.INGRESO_EXITOSO
                    )

                    onLoggedIn()
                }

                is AuthResult.Invalid -> {
                    state.value = state.value.copy(
                        loadingLogin = false,
                        errorMessage = loginResult.reason
                    )
                }
            }
        }
    }

    fun onSyncClick() {
        val s = state.value
        if (s.loadingSync) return

        if (!settings.deviceActivated.value) {
            state.value = s.copy(
                loadingSync = false,
                errorMessage = AppMessages.Device.NO_ACTIVADO,
                goToSettings = true
            )
            return
        }

        state.value = s.copy(
            loadingSync = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val users = withContext(Dispatchers.IO) {
                runCatching { syncLazy.get().syncUsers() }
                    .onFailure { it.printStackTrace() }
                    .getOrDefault(0)
            }

            val inventarioSync = withContext(Dispatchers.IO) {
                runCatching { syncLazy.get().syncAllInventarioPendiente() }
                    .onFailure { it.printStackTrace() }
                    .getOrDefault(
                        InventarioSyncSummary(
                            capturas = 0,
                            finalizados = 0
                        )
                    )
            }

            state.value = state.value.copy(
                loadingSync = false,
                successMessage = AppMessages.Login.resumenSincronizacion(
                    users = users,
                    capturas = inventarioSync.capturas,
                    finalizados = inventarioSync.finalizados
                )
            )
        }
    }

    fun onKeyFileSelected(uriString: String) {
        viewModelScope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    KeyFileReader.readFromUri(ctx, uriString)
                }.getOrNull()
            }

            if (raw.isNullOrBlank()) {
                state.value = state.value.copy(
                    loadingBoot = false,
                    errorMessage = AppMessages.Credentials.NO_SE_PUDO_LEER_ARCHIVO
                )
                return@launch
            }

            val (tokenFromFile, apiKeyFromFile) =
                KeyFileReader.extractAuthTokenAndApiKey(raw)

            if (tokenFromFile.isNullOrBlank() || apiKeyFromFile.isNullOrBlank()) {
                state.value = state.value.copy(
                    loadingBoot = false,
                    errorMessage = AppMessages.Credentials.ARCHIVO_SIN_CREDENCIALES_VALIDAS
                )
                return@launch
            }

            val saveResult = withContext(Dispatchers.IO) {
                runCatching {
                    settings.save(
                        baseUrl = settings.baseUrl.value,
                        empresaRut = settings.empresaRut.value,
                        authToken = tokenFromFile,
                        apiKey = apiKeyFromFile
                    )
                }
            }

            saveResult.onFailure { t ->
                android.util.Log.e("LOGIN_BOOT", "settings.save failed", t)
                state.value = state.value.copy(
                    loadingBoot = false,
                    errorMessage = AppMessages.Credentials.noSePudoGuardar(t.message)
                )
                return@launch
            }

            state.value = state.value.copy(
                loadingBoot = true,
                errorMessage = null
            )

            warmUp()
        }
    }

    fun onUserFocusLost() {
        val formatted = RutInput.formatForDisplay(state.value.username)

        state.value = state.value.copy(
            username = formatted
        )
    }

    fun clearGoToSettings() {
        state.value = state.value.copy(goToSettings = false)
    }

    fun clearMessages() {
        state.value = state.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
