package com.carbroz.partner.infrastructure.persistence.secure

internal interface SecureKeyValueStorage {
    suspend fun read(key: String): SecureStorageResult
    suspend fun write(key: String, value: String): SecureStorageResult
    suspend fun remove(key: String): SecureStorageResult
}

internal sealed interface SecureStorageResult {
    data class Success(val value: String = "") : SecureStorageResult
    data object NotFound : SecureStorageResult
    data class Failure(val message: String) : SecureStorageResult
}
