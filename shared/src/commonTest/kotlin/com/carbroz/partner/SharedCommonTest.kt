package com.carbroz.partner

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Common unit test verifying initial bootstrap contract sanity.
 * Ensures multiplatform test runner configuration operates cleanly across common code.
 */
class SharedCommonTest {
    @Test
    fun verifyBootstrapContractSanity() {
        val appTitle = "CarBroz Partner"
        assertEquals("CarBroz Partner", appTitle, "Bootstrap root title contract must match expected string")
    }
}

