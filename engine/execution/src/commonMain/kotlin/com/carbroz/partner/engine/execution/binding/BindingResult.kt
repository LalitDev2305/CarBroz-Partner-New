package com.carbroz.partner.engine.execution.binding

import com.carbroz.partner.domain.actions.value.ActionValue

/**
 * Universal sealed outcome hierarchy for runtime expression binding resolution.
 */
sealed interface BindingResult {

    data class Success(
        val value: ActionValue
    ) : BindingResult

    data class Failure(
        val reason: String
    ) : BindingResult {
        init {
            require(reason.isNotBlank()) { "BindingResult.Failure reason must not be blank" }
        }
    }
}
