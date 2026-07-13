package com.diprotec.inventario.core.key

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceKeyLogger @Inject constructor(
    private val keyStoreManager: DeviceKeyStoreManager,
    private val publicKeyExporter: DevicePublicKeyExporter
) {

    fun logKeys(alias: String = DeviceKeyConstants.DEVICE_KEY_ALIAS) {
        val privateKey = keyStoreManager.getPrivateKey(alias)
        val publicPem = publicKeyExporter.exportPublicKeyPem(alias)


    }
}