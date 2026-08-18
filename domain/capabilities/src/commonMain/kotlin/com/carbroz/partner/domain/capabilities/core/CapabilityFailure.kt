package com.carbroz.partner.domain.capabilities.core

/**
 * Structured failure descriptor for domain capability operations without leaking platform exceptions.
 */
data class CapabilityFailure(
    val code: FailureCode,
    val message: String
) {
    init {
        require(message.isNotBlank()) { "CapabilityFailure message must not be blank or empty" }
    }

    enum class FailureCode {
        SERVICE_DISABLED,
        TIMEOUT,
        RESOURCE_EXHAUSTED,
        UNKNOWN
    }
}
