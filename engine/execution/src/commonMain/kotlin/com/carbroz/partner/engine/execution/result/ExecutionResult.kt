package com.carbroz.partner.engine.execution.result

import com.carbroz.partner.engine.execution.action.ActionValue

/**
 * Universal sealed outcome hierarchy for dynamic action execution.
 */
sealed interface ExecutionResult {

    data class Success(
        val output: ActionValue = ActionValue.Null
    ) : ExecutionResult

    data class Failure(
        val failure: ExecutionFailure
    ) : ExecutionResult
}
