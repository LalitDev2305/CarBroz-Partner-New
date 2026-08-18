package com.carbroz.partner.domain.capabilities.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CapabilityResultTest {

    @Test
    fun verifyResultSealedVariants() {
        val success = CapabilityResult.Success("value_123")
        val cancelled = CapabilityResult.Cancelled("User closed modal")
        val unavailable = CapabilityResult.Unavailable("GPS disabled")
        val unsupported = CapabilityResult.Unsupported("Desktop platform unsupported")
        val failure = CapabilityResult.Failure(CapabilityFailure(CapabilityFailure.FailureCode.TIMEOUT, "Operation timed out"))

        assertEquals("value_123", success.value)
        assertEquals("User closed modal", cancelled.reason)
        assertEquals("GPS disabled", unavailable.reason)
        assertEquals("Desktop platform unsupported", unsupported.reason)
        assertEquals(CapabilityFailure.FailureCode.TIMEOUT, failure.failure.code)
    }

    @Test
    fun verifyCapabilityFailureBlankMessageRejection() {
        assertFailsWith<IllegalArgumentException> {
            CapabilityFailure(CapabilityFailure.FailureCode.UNKNOWN, "")
        }
        assertFailsWith<IllegalArgumentException> {
            CapabilityFailure(CapabilityFailure.FailureCode.UNKNOWN, "   ")
        }
    }
}
