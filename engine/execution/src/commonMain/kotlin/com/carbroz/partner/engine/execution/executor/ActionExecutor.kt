package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.result.ExecutionResult

/**
 * Domain strategy contract for executing a single supported ActionType.
 */
interface ActionExecutor {
    val supportedType: ActionType
    suspend fun execute(action: ActionSpec): ExecutionResult
}
