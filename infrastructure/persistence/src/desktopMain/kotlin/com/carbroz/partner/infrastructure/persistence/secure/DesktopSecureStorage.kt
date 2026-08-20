package com.carbroz.partner.infrastructure.persistence.secure

/**
 * Desktop JVM process-memory implementation of [SecureKeyValueStorage].
 */
internal class DesktopSecureStorage : SecureKeyValueStorage {

    private val lock = Any()
    private val memoryStore = mutableMapOf<String, String>()

    override suspend fun read(key: String): SecureStorageResult {
        return synchronized(lock) {
            val value = memoryStore[key]
            if (value != null) {
                SecureStorageResult.Success(value)
            } else {
                SecureStorageResult.NotFound
            }
        }
    }

    override suspend fun write(key: String, value: String): SecureStorageResult {
        synchronized(lock) {
            memoryStore[key] = value
        }
        return SecureStorageResult.Success()
    }

    override suspend fun remove(key: String): SecureStorageResult {
        return synchronized(lock) {
            memoryStore.remove(key)
            SecureStorageResult.Success()
        }
    }
}
