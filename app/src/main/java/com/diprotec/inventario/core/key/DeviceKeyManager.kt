package com.diprotec.inventario.core.key

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceKeyManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_ALIAS = "device_identity_key"
    }

    fun ensureKeyPair(alias: String = DEFAULT_ALIAS) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) return

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY or
                    KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun getPublicKeyBase64(alias: String = DEFAULT_ALIAS): String {
        ensureKeyPair(alias)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val cert = keyStore.getCertificate(alias)
            ?: throw IllegalStateException("No existe certificado para alias=$alias")

        return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
    }

    fun getAlias(): String = DEFAULT_ALIAS
}