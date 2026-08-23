package com.carbroz.data.securestorage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android [SecureStorage] backed by an AES key held in Android Keystore.
 *
 * Ciphertext and per-value IVs are stored in private SharedPreferences; the encryption key never
 * leaves Android Keystore. Authentication is intentionally not required so session restoration and
 * legitimate background token work can operate after process recreation without prompting the user.
 */
class AndroidKeystoreSecureStorage(
    context: Context,
    namespace: String,
) : SecureStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        "carbroz.secure.$namespace",
        Context.MODE_PRIVATE,
    )
    private val keyAlias = "carbroz.secure.$namespace.aes"
    private val keyLock = Any()

    init {
        require(namespace.matches(NAMESPACE_PATTERN)) {
            "Secure-storage namespace must contain 3-64 lowercase letters, digits, dot, underscore, or dash."
        }
    }

    override suspend fun read(key: SecureKey): String? {
        val encoded = preferences.getString(key.value, null) ?: return null
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > IV_LENGTH_BYTES) { "Stored secure value is malformed." }

        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        return cipher.doFinal(ciphertext).decodeToString()
    }

    override suspend fun write(key: SecureKey, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val ciphertext = cipher.doFinal(value.encodeToByteArray())
        val payload = cipher.iv + ciphertext
        val encoded = Base64.encodeToString(payload, Base64.NO_WRAP)

        check(preferences.edit().putString(key.value, encoded).commit()) {
            "Failed to durably persist encrypted secure value."
        }
    }

    override suspend fun remove(key: SecureKey) {
        check(preferences.edit().remove(key.value).commit()) {
            "Failed to durably remove encrypted secure value."
        }
    }

    override suspend fun clear() {
        check(preferences.edit().clear().commit()) {
            "Failed to durably clear encrypted secure values."
        }
    }

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .setUserAuthenticationRequired(false)
                        .build(),
                )
            }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
        val NAMESPACE_PATTERN = Regex("^[a-z0-9][a-z0-9_.-]{2,63}$")
    }
}
