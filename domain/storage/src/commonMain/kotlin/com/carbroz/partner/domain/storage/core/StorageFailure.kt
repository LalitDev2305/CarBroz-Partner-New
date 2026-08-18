package com.carbroz.partner.domain.storage.core

/**
 * Structured failure descriptor for domain storage operations without leaking platform exceptions.
 */
data class StorageFailure(
    val code: FailureCode,
    val message: String
) {
    init {
        require(message.isNotBlank()) { "StorageFailure message must not be blank or empty" }
    }

    enum class FailureCode {
        READ_FAILED,
        WRITE_FAILED,
        DELETE_FAILED,
        SECURITY_HARDWARE_UNAVAILABLE,
        UNKNOWN
    }
}
