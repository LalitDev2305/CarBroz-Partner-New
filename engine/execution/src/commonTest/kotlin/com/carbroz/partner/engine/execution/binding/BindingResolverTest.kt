package com.carbroz.partner.engine.execution.binding

import com.carbroz.partner.engine.execution.action.ActionValue
import com.carbroz.partner.engine.execution.binding.BindingExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BindingResolverTest {

    private val resolver = DefaultBindingResolver()

    @Test
    fun verifySimplePathResolutionSuccess() {
        val expression = BindingExpression("\${partnerId}")
        val scope = BindingScope { path ->
            if (path == "partnerId") ActionValue.Text("PRT_99") else null
        }

        val result = resolver.resolve(expression, scope)
        assertTrue(result is BindingResult.Success)
        assertEquals(ActionValue.Text("PRT_99"), result.value)
    }

    @Test
    fun verifyNestedPathResolutionSuccess() {
        val expression = BindingExpression("\${session.user.id}")
        val scope = BindingScope { path ->
            if (path == "session.user.id") ActionValue.Integer(42L) else null
        }

        val result = resolver.resolve(expression, scope)
        assertTrue(result is BindingResult.Success)
        assertEquals(ActionValue.Integer(42L), result.value)
    }

    @Test
    fun verifyMissingPathReturnsFailure() {
        val expression = BindingExpression("\${missing.key}")
        val scope = BindingScope { null }

        val result = resolver.resolve(expression, scope)
        assertTrue(result is BindingResult.Failure)
        assertTrue(result.reason.contains("missing.key"))
    }
}
