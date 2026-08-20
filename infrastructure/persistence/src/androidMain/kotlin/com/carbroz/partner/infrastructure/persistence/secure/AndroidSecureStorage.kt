package com.carbroz.partner.infrastructure.persistence.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android KeyStore-backed AES/GCM implementation of [SecureKeyValueStorage].
 */
internal class AndroidSecureStorage(
    context: Context,
    fileName: String = "carbroz_partner_secure_session"
) : SecureKeyValueStorage {

    private companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "carbroz_partner_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PAYLOAD_PREFIX = "v1"
    }

    private val mutex = Mutex()
    private val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
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

    override suspend fun read(key: String): SecureStorageResult {
        return mutex.withLock {
            try {
                if (!sharedPreferences.contains(key)) {
                    return@withLock SecureStorageResult.NotFound
                }

                val storedPayload = sharedPreferences.getString(key, null)
                    ?: return@withLock SecureStorageResult.NotFound

                val parts = storedPayload.split(":")
                if (parts.size != 3 || parts[0] != PAYLOAD_PREFIX) {
                    return@withLock SecureStorageResult.Failure
                }

                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val cipherText = Base64.decode(parts[2], Base64.NO_WRAP)

                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

                val decryptedBytes = cipher.doFinal(cipherText)
                SecureStorageResult.Success(String(decryptedBytes, Charsets.UTF_8))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                SecureStorageResult.Failure
            }
        }
    }

    override suspend fun write(key: String, value: String): SecureStorageResult {
        return mutex.withLock {
            try {
                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)

                val iv = cipher.iv
                val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

                val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
                val encodedCipherText = Base64.encodeToString(cipherText, Base64.NO_WRAP)
                val payload = "$PAYLOAD_PREFIX:$encodedIv:$encodedCipherText"

                val success = sharedPreferences.edit().putString(key, payload).commit()
                if (success) {
                    SecureStorageResult.Success()
                } else {
                    SecureStorageResult.Failure
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                SecureStorageResult.Failure
            }
        }
    }

    override suspend fun remove(key: String): SecureStorageResult {
        return mutex.withLock {
            try {
                val success = sharedPreferences.edit().remove(key).commit()
                if (success) {
                    SecureStorageResult.Success()
                } else {
                    SecureStorageResult.Failure
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                SecureStorageResult.Failure
            }
        }
    }
}
