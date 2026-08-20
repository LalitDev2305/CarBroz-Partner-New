package com.carbroz.partner.core.navigation

/**
 * Outcome hierarchy of navigation command execution.
 */
sealed interface NavResult {
    data class Executed(val activeEntry: NavEntry) : NavResult

    sealed interface Rejected : NavResult {
        data object CannotPopRoot : Rejected
        data class TargetNotFound(val target: PopToTarget) : Rejected
    }
}
