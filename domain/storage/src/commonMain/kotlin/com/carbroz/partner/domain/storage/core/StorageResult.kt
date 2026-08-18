package com.carbroz.partner.domain.storage.core

/**
 * Universal sealed outcome hierarchy for domain storage operations.
 */
sealed interface StorageResult<out T> {

    data class Success<out T>(
        val value: T
    ) : StorageResult<T>

    data object NotFound : StorageResult<Nothing>

    data class Failure(
        val failure: StorageFailure
    ) : StorageResult<Nothing>
}
