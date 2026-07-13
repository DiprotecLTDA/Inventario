package com.diprotec.inventario.core.key

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceKeyStoreManager @Inject constructor() {

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(DeviceKeyConstants.ANDROID_KEYSTORE).apply {
            load(null)
        }

    fun getAlias(): String = DeviceKeyConstants.DEVICE_KEY_ALIAS

    fun exists(alias: String = getAlias()): Boolean {
        return keyStore().containsAlias(alias)
    }

    fun ensureKeyPair(alias: String = getAlias()) {
        if (exists(alias)) return

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            DeviceKeyConstants.ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(DeviceKeyConstants.KEY_SIZE)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun getPublicKey(alias: String = getAlias()): PublicKey {
        val cert = keyStore().getCertificate(alias)
            ?: error("No se encontró certificado para alias: $alias")

        return cert.publicKey
    }

    fun getPrivateKey(alias: String = getAlias()): PrivateKey {
        val key = keyStore().getKey(alias, null) as? PrivateKey
        return key ?: error("No se encontró PrivateKey para alias: $alias")
    }
}