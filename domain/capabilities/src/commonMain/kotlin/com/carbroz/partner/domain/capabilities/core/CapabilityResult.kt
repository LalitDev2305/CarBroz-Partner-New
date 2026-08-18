package com.carbroz.partner.domain.capabilities.core

/**
 * Universal sealed outcome hierarchy for domain capability operations.
 */
sealed interface CapabilityResult<out T> {

    data class Success<out T>(
        val value: T
    ) : CapabilityResult<T>

    data class Cancelled(
        val reason: String = "User cancelled operation"
    ) : CapabilityResult<Nothing>

    data class Unavailable(
        val reason: String
    ) : CapabilityResult<Nothing>

    data class Unsupported(
        val reason: String
    ) : CapabilityResult<Nothing>

    data class Failure(
        val failure: CapabilityFailure
    ) : CapabilityResult<Nothing>
}
