package com.carbroz.partner.core.navigation.result

import com.carbroz.partner.core.navigation.command.PopToTarget
import com.carbroz.partner.core.navigation.stack.NavEntry

/**
 * Outcome hierarchy of navigation command execution.
 */
sealed interface NavResult {
    data class Executed(val activeEntry: NavEntry) : NavResult

    sealed interface Rejected : NavResult {
        data object CannotPopRoot : Rejected
        data class TargetNotFound(val target: PopToTarget) : Rejected
        data class InvalidRoute(val route: String) : Rejected
    }
}
