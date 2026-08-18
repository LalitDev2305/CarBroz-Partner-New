package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.domain.actions.model.ActionType
import com.carbroz.partner.domain.actions.spec.ActionSpec
import com.carbroz.partner.engine.execution.result.ExecutionResult

/**
 * Domain strategy contract for executing a single supported ActionType.
 */
interface ActionExecutor {
    val supportedType: ActionType
    suspend fun execute(action: ActionSpec): ExecutionResult
}
