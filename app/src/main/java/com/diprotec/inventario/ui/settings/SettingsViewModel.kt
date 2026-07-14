package com.diprotec.inventario.ui.settings

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.service.ActivateDeviceService
import com.diprotec.inventario.core.device.GetSerialNumber
import com.diprotec.inventario.core.key.DeviceKeyStoreManager
import com.diprotec.inventario.core.key.DevicePublicKeyExporter
import com.diprotec.inventario.core.key.KeyFileReader
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.validator.RutValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val baseUrl: String = "",
    val empresaRut: String = "",
    val activationCode: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsManager,
    private val activateDeviceService: ActivateDeviceService,
    private val keyStoreManager: DeviceKeyStoreManager,
    private val publicKeyExporter: DevicePublicKeyExporter,
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    private val _state = mutableStateOf(
        SettingsUiState(
            baseUrl = settings.baseUrl.value,
            empresaRut = settings.empresaRut.value,
            activationCode = settings.activationCode.value
        )
    )

    val state: State<SettingsUiState> = _state

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearInfo() {
        _state.value = _state.value.copy(info = null)
    }

    fun hasCredentials(): Boolean {
        return settings.authToken.value.isNotBlank() &&
                settings.apiKey.value.isNotBlank()
    }

    fun isDeviceActivated(): Boolean {
        return settings.deviceActivated.value
    }

    fun onBaseUrlChange(v: String) {
        _state.value = _state.value.copy(
            baseUrl = v,
            error = null,
            info = null
        )
    }

    fun onEmpresaChange(v: String) {
        _state.value = _state.value.copy(
            empresaRut = v,
            error = null,
            info = null
        )
    }

    fun onActivationCodeChange(v: String) {
        _state.value = _state.value.copy(
            activationCode = v,
            error = null,
            info = null
        )
    }

    fun onSave(onDone: () -> Unit) {
        val s = _state.value
        val base = s.baseUrl.trim()
        val emp = s.empresaRut.trim()
        val activationCode = s.activationCode.trim()

        if (base.isBlank()) {
            _state.value = s.copy(
                error = AppMessages.Settings.INGRESE_BASE_URL,
                info = null,
                saving = false
            )
            return
        }

        if (!(base.startsWith("http://") || base.startsWith("https://"))) {
            _state.value = s.copy(
                error = AppMessages.Settings.BASE_URL_PROTOCOLO,
                info = null,
                saving = false
            )
            return
        }

        if (!base.endsWith("/")) {
            _state.value = s.copy(
                error = AppMessages.Settings.BASE_URL_SLASH_FINAL,
                info = null,
                saving = false
            )
            return
        }

        if (emp.isBlank()) {
            _state.value = s.copy(
                error = AppMessages.Settings.INGRESE_RUT_EMPRESA,
                info = null,
                saving = false
            )
            return
        }

        val normalizedRut = RutValidator.validateAndNormalize(emp)
        if (normalizedRut == null) {
            _state.value = s.copy(
                error = AppMessages.Settings.RUT_EMPRESA_INVALIDO,
                info = null,
                saving = false
            )
            return
        }

        if (!hasCredentials()) {
            _state.value = s.copy(
                error = AppMessages.Settings.CARGUE_ARCHIVO_CREDENCIALES,
                info = null,
                saving = false
            )
            return
        }

        if (!settings.deviceActivated.value && activationCode.isBlank()) {
            _state.value = s.copy(
                error = AppMessages.Settings.INGRESE_ACTIVATION_CODE,
                info = null,
                saving = false
            )
            return
        }

        _state.value = s.copy(
            empresaRut = normalizedRut,
            saving = true,
            error = null,
            info = null
        )

        viewModelScope.launch {
            runCatching {
                if (!settings.deviceActivated.value) {
                    val serialResult = withContext(Dispatchers.IO) {
                        GetSerialNumber(ctx)
                    }

                    val serial = serialResult.serial.orEmpty().trim()
                    if (serial.isBlank()) {
                        throw IllegalStateException(
                            "No se pudo obtener serial: ${serialResult.errMsg ?: "sin detalle"}"
                        )
                    }

                    keyStoreManager.ensureKeyPair()
                    val publicKeyPem = publicKeyExporter.exportPublicKeyPem()
                    val keyAlias = keyStoreManager.getAlias()

                    val deviceId = activateDeviceService.activate(
                        empresaRut = normalizedRut,
                        serialNumber = serial,
                        activationCode = activationCode,
                        publicKey = publicKeyPem
                    )

                    settings.save(
                        baseUrl = base,
                        empresaRut = normalizedRut,
                        authToken = settings.authToken.value,
                        apiKey = settings.apiKey.value,
                        deviceSession = settings.deviceSession.value,
                        deviceId = deviceId,
                        activationCode = activationCode,
                        deviceKeyAlias = keyAlias,
                        deviceActivated = true
                    )
                } else {
                    settings.save(
                        baseUrl = base,
                        empresaRut = normalizedRut,
                        authToken = settings.authToken.value,
                        apiKey = settings.apiKey.value,
                        deviceSession = settings.deviceSession.value,
                        deviceId = settings.deviceId.value,
                        activationCode = settings.activationCode.value,
                        deviceKeyAlias = settings.deviceKeyAlias.value,
                        deviceActivated = settings.deviceActivated.value
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    error = it.message ?: AppMessages.Settings.ERROR_GUARDANDO_CONFIGURACION,
                    info = null
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    error = null,
                    info = AppMessages.Settings.CONFIGURACION_GUARDADA
                )
                onDone()
            }
        }
    }

    suspend fun importKeyFromUri(uriString: String): Boolean {
        val raw = runCatching {
            KeyFileReader.readFromUri(ctx, uriString)
        }.getOrNull()

        if (raw.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = AppMessages.Credentials.NO_SE_PUDO_LEER_ARCHIVO,
                info = null
            )
            return false
        }

        val (tokenFromFile, apiKeyFromFile) =
            KeyFileReader.extractAuthTokenAndApiKey(raw)

        if (tokenFromFile.isNullOrBlank() || apiKeyFromFile.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = AppMessages.Credentials.ARCHIVO_FORMATO_INESPERADO,
                info = null
            )
            return false
        }

        if (!KeyFileReader.isInventarioToken(tokenFromFile)) {
            val appClaim = KeyFileReader.extractJwtAppClaim(tokenFromFile)
            _state.value = _state.value.copy(
                error = AppMessages.Credentials.tokenNoCorresponde(appClaim),
                info = null
            )
            return false
        }

        val ok = runCatching {
            settings.save(
                baseUrl = settings.baseUrl.value,
                empresaRut = settings.empresaRut.value,
                authToken = tokenFromFile,
                apiKey = apiKeyFromFile,
                deviceSession = settings.deviceSession.value,
                deviceId = settings.deviceId.value,
                activationCode = settings.activationCode.value,
                deviceKeyAlias = settings.deviceKeyAlias.value,
                deviceActivated = settings.deviceActivated.value
            )
        }.isSuccess

        _state.value = if (ok) {
            _state.value.copy(
                error = null,
                info = AppMessages.Credentials.CARGADAS_CORRECTAMENTE
            )
        } else {
            _state.value.copy(
                error = AppMessages.Credentials.ERROR_GUARDANDO,
                info = null
            )
        }

        return ok
    }
}
