package com.carbroz.partner.engine.execution.dispatcher

import com.carbroz.partner.domain.actions.spec.ActionSpec
import com.carbroz.partner.engine.execution.binding.BindingScope
import com.carbroz.partner.engine.execution.result.ExecutionResult

/**
 * Primary entry interface for dispatching and executing dynamic ActionSpecs.
 */
interface ActionDispatcher {
    suspend fun dispatch(
        action: ActionSpec,
        scope: BindingScope? = null
    ): ExecutionResult
}
