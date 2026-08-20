package com.carbroz.partner.infrastructure.persistence.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android KeyStore-backed [EncryptedSharedPreferences] implementation of [SecureKeyValueStorage].
 */
internal class AndroidSecureStorage(
    context: Context,
    fileName: String = "carbroz_partner_secure_session"
) : SecureKeyValueStorage {

    private val mutex = Mutex()
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        fileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun read(key: String): SecureStorageResult {
        return mutex.withLock {
            try {
                if (!sharedPreferences.contains(key)) {
                    SecureStorageResult.NotFound
                } else {
                    val value = sharedPreferences.getString(key, null)
                    if (value != null) {
                        SecureStorageResult.Success(value)
                    } else {
                        SecureStorageResult.NotFound
                    }
                }
            } catch (_: Exception) {
                SecureStorageResult.Failure
            }
        }
    }

    override suspend fun write(key: String, value: String): SecureStorageResult {
        return mutex.withLock {
            try {
                val success = sharedPreferences.edit().putString(key, value).commit()
                if (success) {
                    SecureStorageResult.Success()
                } else {
                    SecureStorageResult.Failure
                }
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
            } catch (_: Exception) {
                SecureStorageResult.Failure
            }
        }
    }
}
