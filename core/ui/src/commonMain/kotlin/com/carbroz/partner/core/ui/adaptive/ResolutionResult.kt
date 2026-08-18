package com.carbroz.partner.core.ui.adaptive

/**
 * Strongly typed resolution outcome contract distinguishing success, recovery, invalid input, and unsupported features.
 */
sealed interface ResolutionResult<out T> {
    data class Resolved<T>(val value: T) : ResolutionResult<T>
    data class Recovered<T>(val fallbackValue: T, val reason: String) : ResolutionResult<T>
    data class Invalid(val reason: String) : ResolutionResult<Nothing>
    data class Unsupported(val reason: String) : ResolutionResult<Nothing>
}
