package com.carbroz.partner.domain.storage.secure

import com.carbroz.partner.domain.storage.core.StorageResult

/**
 * Domain gateway for reading, writing, and removing hardware-encrypted sensitive string payloads.
 */
interface SecureStorageGateway {
    suspend fun getSecret(key: String): StorageResult<String>
    suspend fun putSecret(key: String, value: String): StorageResult<Unit>
    suspend fun removeSecret(key: String): StorageResult<Unit>
}
