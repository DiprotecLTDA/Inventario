package com.diprotec.inventario.core.key

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceKeyInitializer @Inject constructor(
    private val keyStoreManager: DeviceKeyStoreManager,
    private val keyLogger: DeviceKeyLogger
) {

    fun initializeAndLog(alias: String = DeviceKeyConstants.DEVICE_KEY_ALIAS) {
        keyStoreManager.ensureKeyPair(alias)

        runCatching {
            keyLogger.logKeys(alias)
        }.onFailure { t ->
            Log.w(TAG, "No se pudo registrar información de la llave: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "DeviceKeyInitializer"
    }
}