package com.carbroz.partner.domain.actions.binding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BindingExpressionTest {

    @Test
    fun verifyValidBindingExpression() {
        val expr = BindingExpression("\${session.partnerId}")
        assertEquals("\${session.partnerId}", expr.rawExpression)
    }

    @Test
    fun verifyMalformedBindingExpressionRejection() {
        assertFailsWith<IllegalArgumentException> {
            BindingExpression("session.partnerId") // Missing ${ and }
        }
        assertFailsWith<IllegalArgumentException> {
            BindingExpression("\${}") // Empty inner expression
        }
        assertFailsWith<IllegalArgumentException> {
            BindingExpression("\${   }") // Whitespace inner expression
        }
        assertFailsWith<IllegalArgumentException> {
            BindingExpression("") // Blank
        }
    }
}
