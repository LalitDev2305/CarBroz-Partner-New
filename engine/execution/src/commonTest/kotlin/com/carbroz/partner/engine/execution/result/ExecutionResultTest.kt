package com.carbroz.partner.engine.execution.result

import com.carbroz.partner.domain.actions.value.ActionValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecutionResultTest {

    @Test
    fun verifySuccessVariantDefaultsToNullValue() {
        val success = ExecutionResult.Success()
        assertEquals(ActionValue.Null, success.output)
    }

    @Test
    fun verifyFailureVariantRetainsFailure() {
        val failure = ExecutionFailure(ExecutionFailure.FailureCode.UNREGISTERED_ACTION_TYPE, "Unknown action")
        val result = ExecutionResult.Failure(failure)
        assertEquals(ExecutionFailure.FailureCode.UNREGISTERED_ACTION_TYPE, result.failure.code)
        assertEquals("Unknown action", result.failure.message)
    }

    @Test
    fun verifyBlankFailureMessageRejected() {
        assertFailsWith<IllegalArgumentException> {
            ExecutionFailure(ExecutionFailure.FailureCode.UNKNOWN, "")
        }
        assertFailsWith<IllegalArgumentException> {
            ExecutionFailure(ExecutionFailure.FailureCode.UNKNOWN, "   ")
        }
    }
}
