package com.diprotec.inventario.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val baseUrl: String,
    val empresaRut: String,
    val authToken: String,
    val apiKey: String,
    val deviceSession: String,
    val deviceId: String,
    val activationCode: String,
    val deviceKeyAlias: String,
    val deviceActivated: Boolean,
    val pendingUpdateMandatory: Boolean,
    val pendingDownloadId: Long,
    val pendingApkFileName: String,
    val pendingApkUrl: String,
    val apkDownloaded: Boolean,
    val sessionRut: String,
    val sessionPerfilId: Int,
    val sessionLastActivityAt: Long,
    val sessionBootElapsedRealtime: Long,
    val sessionActive: Boolean
)

class SettingsDataStore(private val ctx: Context) {

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_EMPRESA_RUT = stringPreferencesKey("empresa_rut")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_DEVICE_SESSION = stringPreferencesKey("device_session")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_ACTIVATION_CODE = stringPreferencesKey("activation_code")
        private val KEY_DEVICE_KEY_ALIAS = stringPreferencesKey("device_key_alias")
        private val KEY_DEVICE_ACTIVATED = booleanPreferencesKey("device_activated")

        private val KEY_PENDING_UPDATE_MANDATORY =
            booleanPreferencesKey("pending_update_mandatory")
        private val KEY_PENDING_DOWNLOAD_ID =
            longPreferencesKey("pending_download_id")
        private val KEY_PENDING_APK_FILE_NAME =
            stringPreferencesKey("pending_apk_file_name")
        private val KEY_PENDING_APK_URL =
            stringPreferencesKey("pending_apk_url")
        private val KEY_APK_DOWNLOADED =
            booleanPreferencesKey("apk_downloaded")

        private val KEY_SESSION_RUT =
            stringPreferencesKey("session_rut")
        private val KEY_SESSION_PERFIL_ID =
            intPreferencesKey("session_perfil_id")
        private val KEY_SESSION_LAST_ACTIVITY_AT =
            longPreferencesKey("session_last_activity_at")
        private val KEY_SESSION_BOOT_ELAPSED_REALTIME =
            longPreferencesKey("session_boot_elapsed_realtime")
        private val KEY_SESSION_ACTIVE =
            booleanPreferencesKey("session_active")
    }

    val settingsFlow: Flow<AppSettings> = ctx.dataStore.data.map { prefs ->
        AppSettings(
            baseUrl = prefs[KEY_BASE_URL] ?: "",
            empresaRut = prefs[KEY_EMPRESA_RUT] ?: "",
            authToken = prefs[KEY_AUTH_TOKEN] ?: "",
            apiKey = prefs[KEY_API_KEY] ?: "",
            deviceSession = prefs[KEY_DEVICE_SESSION] ?: "",
            deviceId = prefs[KEY_DEVICE_ID] ?: "",
            activationCode = prefs[KEY_ACTIVATION_CODE] ?: "",
            deviceKeyAlias = prefs[KEY_DEVICE_KEY_ALIAS] ?: "",
            deviceActivated = prefs[KEY_DEVICE_ACTIVATED] ?: false,
            pendingUpdateMandatory = prefs[KEY_PENDING_UPDATE_MANDATORY] ?: false,
            pendingDownloadId = prefs[KEY_PENDING_DOWNLOAD_ID] ?: -1L,
            pendingApkFileName = prefs[KEY_PENDING_APK_FILE_NAME] ?: "",
            pendingApkUrl = prefs[KEY_PENDING_APK_URL] ?: "",
            apkDownloaded = prefs[KEY_APK_DOWNLOADED] ?: false,
            sessionRut = prefs[KEY_SESSION_RUT] ?: "",
            sessionPerfilId = prefs[KEY_SESSION_PERFIL_ID] ?: -1,
            sessionLastActivityAt = prefs[KEY_SESSION_LAST_ACTIVITY_AT] ?: 0L,
            sessionBootElapsedRealtime = prefs[KEY_SESSION_BOOT_ELAPSED_REALTIME] ?: 0L,
            sessionActive = prefs[KEY_SESSION_ACTIVE] ?: false
        )
    }

    suspend fun save(
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
        ctx.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = normalizeBaseUrl(baseUrl)
            prefs[KEY_EMPRESA_RUT] = empresaRut.trim()
            prefs[KEY_AUTH_TOKEN] = authToken.trim()
            prefs[KEY_API_KEY] = apiKey.trim()
            prefs[KEY_DEVICE_SESSION] = deviceSession.trim()
            prefs[KEY_DEVICE_ID] = deviceId.trim()
            prefs[KEY_ACTIVATION_CODE] = activationCode.trim()
            prefs[KEY_DEVICE_KEY_ALIAS] = deviceKeyAlias.trim()
            prefs[KEY_DEVICE_ACTIVATED] = deviceActivated
        }
    }

    suspend fun saveDeviceSession(value: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_SESSION] = value.trim()
        }
    }

    suspend fun saveDeviceId(value: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = value.trim()
        }
    }

    suspend fun saveActivation(
        activationCode: String,
        deviceKeyAlias: String,
        deviceActivated: Boolean
    ) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_ACTIVATION_CODE] = activationCode.trim()
            prefs[KEY_DEVICE_KEY_ALIAS] = deviceKeyAlias.trim()
            prefs[KEY_DEVICE_ACTIVATED] = deviceActivated
        }
    }

    suspend fun savePendingUpdate(
        mandatory: Boolean,
        downloadId: Long,
        apkFileName: String,
        apkUrl: String,
        apkDownloaded: Boolean
    ) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_PENDING_UPDATE_MANDATORY] = mandatory
            prefs[KEY_PENDING_DOWNLOAD_ID] = downloadId
            prefs[KEY_PENDING_APK_FILE_NAME] = apkFileName.trim()
            prefs[KEY_PENDING_APK_URL] = apkUrl.trim()
            prefs[KEY_APK_DOWNLOADED] = apkDownloaded
        }
    }

    suspend fun updatePendingDownloadState(
        downloadId: Long? = null,
        apkDownloaded: Boolean? = null
    ) {
        ctx.dataStore.edit { prefs ->
            downloadId?.let { prefs[KEY_PENDING_DOWNLOAD_ID] = it }
            apkDownloaded?.let { prefs[KEY_APK_DOWNLOADED] = it }
        }
    }

    suspend fun clearPendingUpdate() {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_PENDING_UPDATE_MANDATORY] = false
            prefs[KEY_PENDING_DOWNLOAD_ID] = -1L
            prefs[KEY_PENDING_APK_FILE_NAME] = ""
            prefs[KEY_PENDING_APK_URL] = ""
            prefs[KEY_APK_DOWNLOADED] = false
        }
    }

    suspend fun saveUserSession(
        rut: String,
        perfilId: Int?,
        lastActivityAt: Long,
        bootElapsedRealtime: Long,
        active: Boolean
    ) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_SESSION_RUT] = rut.trim()
            prefs[KEY_SESSION_PERFIL_ID] = perfilId ?: -1
            prefs[KEY_SESSION_LAST_ACTIVITY_AT] = lastActivityAt
            prefs[KEY_SESSION_BOOT_ELAPSED_REALTIME] = bootElapsedRealtime
            prefs[KEY_SESSION_ACTIVE] = active
        }
    }

    suspend fun updateUserSessionActivity(lastActivityAt: Long) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_SESSION_LAST_ACTIVITY_AT] = lastActivityAt
        }
    }

    suspend fun clearUserSession() {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_SESSION_RUT] = ""
            prefs[KEY_SESSION_PERFIL_ID] = -1
            prefs[KEY_SESSION_LAST_ACTIVITY_AT] = 0L
            prefs[KEY_SESSION_BOOT_ELAPSED_REALTIME] = 0L
            prefs[KEY_SESSION_ACTIVE] = false
        }
    }

    private fun normalizeBaseUrl(v: String): String {
        val s = v.trim()
        return if (s.endsWith("/")) s else "$s/"
    }
}