package com.diprotec.inventario.core.config

import android.content.Context
import com.diprotec.inventario.data.local.datastore.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext ctx: Context
) {
    private val store = SettingsDataStore(ctx)

    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl

    private val _empresaRut = MutableStateFlow("")
    val empresaRut: StateFlow<String> = _empresaRut

    private val _authToken = MutableStateFlow("")
    val authToken: StateFlow<String> = _authToken

    private val _deviceSession = MutableStateFlow("")
    val deviceSession: StateFlow<String> = _deviceSession

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _activationCode = MutableStateFlow("")
    val activationCode: StateFlow<String> = _activationCode

    private val _deviceKeyAlias = MutableStateFlow("")
    val deviceKeyAlias: StateFlow<String> = _deviceKeyAlias

    private val _deviceActivated = MutableStateFlow(false)
    val deviceActivated: StateFlow<Boolean> = _deviceActivated

    private val _pendingUpdateMandatory = MutableStateFlow(false)
    val pendingUpdateMandatory: StateFlow<Boolean> = _pendingUpdateMandatory

    private val _pendingDownloadId = MutableStateFlow(-1L)
    val pendingDownloadId: StateFlow<Long> = _pendingDownloadId

    private val _pendingApkFileName = MutableStateFlow("")
    val pendingApkFileName: StateFlow<String> = _pendingApkFileName

    private val _pendingApkUrl = MutableStateFlow("")
    val pendingApkUrl: StateFlow<String> = _pendingApkUrl

    private val _apkDownloaded = MutableStateFlow(false)
    val apkDownloaded: StateFlow<Boolean> = _apkDownloaded

    private val _sessionRut = MutableStateFlow("")
    val sessionRut: StateFlow<String> = _sessionRut

    private val _sessionPerfilId = MutableStateFlow(-1)
    val sessionPerfilId: StateFlow<Int> = _sessionPerfilId

    private val _sessionLastActivityAt = MutableStateFlow(0L)
    val sessionLastActivityAt: StateFlow<Long> = _sessionLastActivityAt

    private val _sessionBootElapsedRealtime = MutableStateFlow(0L)
    val sessionBootElapsedRealtime: StateFlow<Long> = _sessionBootElapsedRealtime

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive

    fun isConfiguredNow(): Boolean {
        return baseUrl.value.isNotBlank() &&
                empresaRut.value.isNotBlank() &&
                apiKey.value.isNotBlank() &&
                authToken.value.isNotBlank()
    }

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            store.settingsFlow.collect { s ->
                _baseUrl.value = s.baseUrl
                _empresaRut.value = s.empresaRut
                _authToken.value = s.authToken
                _apiKey.value = s.apiKey
                _deviceSession.value = s.deviceSession
                _deviceId.value = s.deviceId
                _activationCode.value = s.activationCode
                _deviceKeyAlias.value = s.deviceKeyAlias
                _deviceActivated.value = s.deviceActivated
                _pendingUpdateMandatory.value = s.pendingUpdateMandatory
                _pendingDownloadId.value = s.pendingDownloadId
                _pendingApkFileName.value = s.pendingApkFileName
                _pendingApkUrl.value = s.pendingApkUrl
                _apkDownloaded.value = s.apkDownloaded
                _sessionRut.value = s.sessionRut
                _sessionPerfilId.value = s.sessionPerfilId
                _sessionLastActivityAt.value = s.sessionLastActivityAt
                _sessionBootElapsedRealtime.value = s.sessionBootElapsedRealtime
                _sessionActive.value = s.sessionActive
            }
        }
    }

    suspend fun save(
        baseUrl: String,
        empresaRut: String,
        authToken: String,
        apiKey: String,
        deviceSession: String = this.deviceSession.value,
        deviceId: String = this.deviceId.value,
        activationCode: String = this.activationCode.value,
        deviceKeyAlias: String = this.deviceKeyAlias.value,
        deviceActivated: Boolean = this.deviceActivated.value
    ) {
        // Actualiza el StateFlow en memoria ANTES de persistir en DataStore. El colector de
        // start() reacciona a la emisión de store.settingsFlow de forma asíncrona: sin esto,
        // código que lee settings.authToken.value inmediatamente después de save() (p.ej.
        // ActivateDeviceService, llamado en el mismo onSave() de Settings) puede ver el valor
        // VIEJO durante esa ventana, y el backend rechaza el token con 401/403 aunque el
        // archivo importado sea válido.
        applyImmediate(
            baseUrl = normalizeBaseUrl(baseUrl),
            empresaRut = empresaRut.trim(),
            authToken = authToken.trim(),
            apiKey = apiKey.trim(),
            deviceSession = deviceSession.trim(),
            deviceId = deviceId.trim(),
            activationCode = activationCode.trim(),
            deviceKeyAlias = deviceKeyAlias.trim(),
            deviceActivated = deviceActivated
        )

        store.save(
            baseUrl = baseUrl,
            empresaRut = empresaRut,
            authToken = authToken,
            apiKey = apiKey,
            deviceSession = deviceSession,
            deviceId = deviceId,
            activationCode = activationCode,
            deviceKeyAlias = deviceKeyAlias,
            deviceActivated = deviceActivated
        )
    }

    suspend fun saveDeviceSession(value: String) {
        _deviceSession.value = value.trim()
        store.saveDeviceSession(value)
    }

    suspend fun saveDeviceId(value: String) {
        _deviceId.value = value.trim()
        store.saveDeviceId(value)
    }

    suspend fun saveActivation(
        activationCode: String,
        deviceKeyAlias: String,
        deviceActivated: Boolean
    ) {
        _activationCode.value = activationCode.trim()
        _deviceKeyAlias.value = deviceKeyAlias.trim()
        _deviceActivated.value = deviceActivated

        store.saveActivation(
            activationCode = activationCode,
            deviceKeyAlias = deviceKeyAlias,
            deviceActivated = deviceActivated
        )
    }

    private fun applyImmediate(
        baseUrl: String,
        empresaRut: String,
        authToken: String,
        apiKey: String,
        deviceSession: String,
        deviceId: String,
        activationCode: String,
        deviceKeyAlias: String,
        deviceActivated: Boolean
    ) {
        _baseUrl.value = baseUrl
        _empresaRut.value = empresaRut
        _authToken.value = authToken
        _apiKey.value = apiKey
        _deviceSession.value = deviceSession
        _deviceId.value = deviceId
        _activationCode.value = activationCode
        _deviceKeyAlias.value = deviceKeyAlias
        _deviceActivated.value = deviceActivated
    }

    private fun normalizeBaseUrl(v: String): String {
        val s = v.trim()
        return if (s.endsWith("/")) s else "$s/"
    }

    suspend fun savePendingUpdate(
        mandatory: Boolean,
        downloadId: Long,
        apkFileName: String,
        apkUrl: String,
        apkDownloaded: Boolean
    ) {
        store.savePendingUpdate(
            mandatory = mandatory,
            downloadId = downloadId,
            apkFileName = apkFileName,
            apkUrl = apkUrl,
            apkDownloaded = apkDownloaded
        )
    }

    suspend fun updatePendingDownloadState(
        downloadId: Long? = null,
        apkDownloaded: Boolean? = null
    ) {
        store.updatePendingDownloadState(
            downloadId = downloadId,
            apkDownloaded = apkDownloaded
        )
    }

    suspend fun clearPendingUpdate() {
        store.clearPendingUpdate()
    }

    suspend fun saveUserSession(
        rut: String,
        perfilId: Int?,
        lastActivityAt: Long,
        bootElapsedRealtime: Long,
        active: Boolean
    ) {
        store.saveUserSession(
            rut = rut,
            perfilId = perfilId,
            lastActivityAt = lastActivityAt,
            bootElapsedRealtime = bootElapsedRealtime,
            active = active
        )
    }

    suspend fun updateUserSessionActivity(lastActivityAt: Long) {
        store.updateUserSessionActivity(lastActivityAt)
    }

    suspend fun clearUserSession() {
        store.clearUserSession()
    }
}