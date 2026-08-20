package com.carbroz.partner.engine.execution.binding

import com.carbroz.partner.engine.execution.binding.BindingExpression

/**
 * Interface contract for resolving BindingExpressions against a runtime BindingScope.
 */
interface BindingResolver {
    fun resolve(expression: BindingExpression, scope: BindingScope): BindingResult
}
