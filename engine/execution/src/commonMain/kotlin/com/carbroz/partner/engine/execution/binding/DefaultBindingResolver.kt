package com.carbroz.partner.engine.execution.binding

import com.carbroz.partner.engine.execution.binding.BindingExpression

/**
 * Standard implementation of BindingResolver that extracts inner paths and delegates to BindingScope.
 */
class DefaultBindingResolver : BindingResolver {
    override fun resolve(expression: BindingExpression, scope: BindingScope): BindingResult {
        val trimmed = expression.rawExpression.trim()
        val path = trimmed.substring(2, trimmed.length - 1).trim()
        val resolved = scope.resolveValue(path)
        return if (resolved != null) {
            BindingResult.Success(resolved)
        } else {
            BindingResult.Failure("Failed to resolve binding path: '$path'")
        }
    }
}
