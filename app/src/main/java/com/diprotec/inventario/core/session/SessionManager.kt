package com.diprotec.inventario.core.session

import android.os.SystemClock
import com.diprotec.inventario.core.config.SettingsManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val settings: SettingsManager
) {

    private val _loginRut = MutableStateFlow<String?>(null)
    val loginRut: StateFlow<String?> = _loginRut

    private val _perfilId = MutableStateFlow<Int?>(null)
    val perfilId: StateFlow<Int?> = _perfilId

    @Volatile
    private var lastActivityWriteAt: Long = 0L

    val sessionLastActivityAt: StateFlow<Long>
        get() = settings.sessionLastActivityAt

    val maxSessionMs: Long
        get() = MAX_SESSION_MS

    fun setLogin(rut: String?) {
        _loginRut.value = rut
    }

    fun setPerfilId(id: Int?) {
        _perfilId.value = id
    }

    suspend fun login(
        rut: String,
        perfilId: Int?
    ) {
        val now = System.currentTimeMillis()
        val bootElapsedRealtime = SystemClock.elapsedRealtime()

        _loginRut.value = rut
        _perfilId.value = perfilId

        settings.saveUserSession(
            rut = rut,
            perfilId = perfilId,
            lastActivityAt = now,
            bootElapsedRealtime = bootElapsedRealtime,
            active = true
        )
    }

    suspend fun restoreSession(): Boolean {
        val rut = settings.sessionRut.value.trim()
        val perfilId = settings.sessionPerfilId.value
        val lastActivityAt = settings.sessionLastActivityAt.value
        val bootElapsedAtLogin = settings.sessionBootElapsedRealtime.value
        val active = settings.sessionActive.value

        if (!active || rut.isBlank() || lastActivityAt <= 0L || bootElapsedAtLogin <= 0L) {
            logout()
            return false
        }

        val deviceWasRebooted = SystemClock.elapsedRealtime() < bootElapsedAtLogin

        if (deviceWasRebooted) {
            logout()
            return false
        }

        val now = System.currentTimeMillis()
        val expired = now - lastActivityAt > MAX_SESSION_MS

        if (expired) {
            logout()
            return false
        }

        _loginRut.value = rut
        _perfilId.value = perfilId

        settings.updateUserSessionActivity(now)

        return true
    }

    suspend fun touchIfValid(): Boolean {
        val rut = _loginRut.value ?: settings.sessionRut.value.trim()
        val lastActivityAt = settings.sessionLastActivityAt.value
        val bootElapsedAtLogin = settings.sessionBootElapsedRealtime.value
        val active = settings.sessionActive.value

        if (!active || rut.isBlank() || lastActivityAt <= 0L || bootElapsedAtLogin <= 0L) {
            return false
        }

        val deviceWasRebooted = SystemClock.elapsedRealtime() < bootElapsedAtLogin

        if (deviceWasRebooted) {
            logout()
            return false
        }

        val now = System.currentTimeMillis()
        val expired = now - lastActivityAt > MAX_SESSION_MS

        if (expired) {
            logout()
            return false
        }

        if (_loginRut.value == null) {
            _loginRut.value = rut
        }

        if (_perfilId.value == null) {
            _perfilId.value = settings.sessionPerfilId.value
        }

        // Throttle: evita escribir a DataStore en cada toque; una vez por segundo alcanza
        // para mantener la sesión viva sin generar I/O excesivo ante interacción continua.
        if (now - lastActivityWriteAt >= ACTIVITY_WRITE_THROTTLE_MS) {
            lastActivityWriteAt = now
            settings.updateUserSessionActivity(now)
        }

        return true
    }

    suspend fun notifyUserActivity(): Boolean {
        return touchIfValid()
    }

    suspend fun logout() {
        _loginRut.value = null
        _perfilId.value = null
        settings.clearUserSession()
    }

    fun clear() {
        _loginRut.value = null
        _perfilId.value = null
    }

    companion object {
        private val MAX_SESSION_MS = TimeUnit.HOURS.toMillis(3)
        private const val ACTIVITY_WRITE_THROTTLE_MS = 1_000L
    }
}