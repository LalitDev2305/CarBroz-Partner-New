package com.carbroz.partner.engine.execution.binding

import com.carbroz.partner.domain.actions.value.ActionValue

/**
 * Functional interface for looking up runtime ActionValues by normalized binding path.
 */
fun interface BindingScope {
    fun resolveValue(path: String): ActionValue?
}
