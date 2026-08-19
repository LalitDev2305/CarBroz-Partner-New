package com.carbroz.partner.infrastructure.persistence.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.carbroz.partner.domain.storage.core.StorageFailure
import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android KeyStore-backed [EncryptedSharedPreferences] implementation of [SecureStorageGateway].
 *
 * Credentials are stored encrypted on disk using AES-256 GCM authenticated encryption,
 * keyed by an AES-256 master key generated in Android KeyStore.
 */
public class AndroidSecureStorage(
    context: Context,
    fileName: String = "carbroz_partner_secure_session"
) : SecureStorageGateway {

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

    override suspend fun getSecret(key: String): StorageResult<String> {
        return mutex.withLock {
            try {
                if (!sharedPreferences.contains(key)) {
                    StorageResult.NotFound
                } else {
                    val value = sharedPreferences.getString(key, null)
                    if (value != null) {
                        StorageResult.Success(value)
                    } else {
                        StorageResult.NotFound
                    }
                }
            } catch (e: Exception) {
                StorageResult.Failure(
                    StorageFailure(
                        code = StorageFailure.FailureCode.READ_FAILED,
                        message = e.message ?: "Failed to read secret from EncryptedSharedPreferences"
                    )
                )
            }
        }
    }

    override suspend fun putSecret(key: String, value: String): StorageResult<Unit> {
        return mutex.withLock {
            try {
                val success = sharedPreferences.edit().putString(key, value).commit()
                if (success) {
                    StorageResult.Success(Unit)
                } else {
                    StorageResult.Failure(
                        StorageFailure(
                            code = StorageFailure.FailureCode.WRITE_FAILED,
                            message = "EncryptedSharedPreferences write commit returned false"
                        )
                    )
                }
            } catch (e: Exception) {
                StorageResult.Failure(
                    StorageFailure(
                        code = StorageFailure.FailureCode.WRITE_FAILED,
                        message = e.message ?: "Failed to write secret to EncryptedSharedPreferences"
                    )
                )
            }
        }
    }

    override suspend fun removeSecret(key: String): StorageResult<Unit> {
        return mutex.withLock {
            try {
                val success = sharedPreferences.edit().remove(key).commit()
                if (success) {
                    StorageResult.Success(Unit)
                } else {
                    StorageResult.Failure(
                        StorageFailure(
                            code = StorageFailure.FailureCode.DELETE_FAILED,
                            message = "EncryptedSharedPreferences remove commit returned false"
                        )
                    )
                }
            } catch (e: Exception) {
                StorageResult.Failure(
                    StorageFailure(
                        code = StorageFailure.FailureCode.DELETE_FAILED,
                        message = e.message ?: "Failed to remove secret from EncryptedSharedPreferences"
                    )
                )
            }
        }
    }
}
