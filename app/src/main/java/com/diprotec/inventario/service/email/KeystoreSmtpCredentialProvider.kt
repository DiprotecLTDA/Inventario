package com.diprotec.inventario.service.email

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.diprotec.inventario.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda la contraseña SMTP cifrada con AES/GCM en Android Keystore (alias
 * [KEY_ALIAS]), sembrada desde `BuildConfig.SMTP_PASSWORD` (credencial inyectada al
 * compilar, nunca en texto plano en el APK) la primera vez que se usa. Si no fue
 * inyectada y no hay nada cacheado, el envío queda deshabilitado sin romper la app.
 */
@Singleton
class KeystoreSmtpCredentialProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : SmtpCredentialProvider {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun getPassword(): CharArray {
        val cached = readCachedCiphertext()

        if (cached != null) {
            return decrypt(cached.ciphertext, cached.iv)
        }

        val fromBuildConfig = BuildConfig.SMTP_PASSWORD

        if (fromBuildConfig.isBlank()) {
            throw MissingSmtpCredentialException()
        }

        val (ciphertext, iv) = encrypt(fromBuildConfig)
        persistCiphertext(ciphertext, iv)

        return fromBuildConfig.toCharArray()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return ciphertext to cipher.iv
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): CharArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )

        val plain = cipher.doFinal(ciphertext)
        val chars = String(plain, Charsets.UTF_8).toCharArray()
        plain.fill(0)

        return chars
    }

    private data class Cached(val ciphertext: ByteArray, val iv: ByteArray)

    private fun readCachedCiphertext(): Cached? {
        val ciphertextB64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null

        return runCatching {
            Cached(
                ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP),
                iv = Base64.decode(ivB64, Base64.NO_WRAP)
            )
        }.getOrNull()
    }

    private fun persistCiphertext(ciphertext: ByteArray, iv: ByteArray) {
        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "smtp_credential_store"
        private const val KEY_CIPHERTEXT = "ciphertext"
        private const val KEY_IV = "iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "unitech_smtp_credential_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
