package com.carbroz.partner.infrastructure.persistence.secure

import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway

/**
 * Desktop JVM process-memory implementation of [SecureStorageGateway].
 *
 * Credentials are stored safely in process memory only ([DESKTOP_SESSION_PERSISTENCE = NON_PERSISTENT])
 * and are never persisted to plain-text files or unprotected local preferences.
 */
public class DesktopSecureStorage : SecureStorageGateway {

    private val lock = Any()
    private val memoryStore = mutableMapOf<String, String>()

    override suspend fun getSecret(key: String): StorageResult<String> {
        return synchronized(lock) {
            val value = memoryStore[key]
            if (value != null) {
                StorageResult.Success(value)
            } else {
                StorageResult.NotFound
            }
        }
    }

    override suspend fun putSecret(key: String, value: String): StorageResult<Unit> {
        synchronized(lock) {
            memoryStore[key] = value
        }
        return StorageResult.Success(Unit)
    }

    override suspend fun removeSecret(key: String): StorageResult<Unit> {
        return synchronized(lock) {
            if (memoryStore.remove(key) != null) {
                StorageResult.Success(Unit)
            } else {
                StorageResult.NotFound
            }
        }
    }
}
