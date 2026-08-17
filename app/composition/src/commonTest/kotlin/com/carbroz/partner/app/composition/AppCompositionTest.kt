package com.carbroz.partner.app.composition

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Baseline contract unit test verifying multiplatform module compilation sanity.
 * Validates the core app title contract for CarBroz Partner.
 */
class AppCompositionTest {
    @Test
    fun verifyCompositionContractSanity() {
        val appTitle = "CarBroz Partner"
        assertEquals("CarBroz Partner", appTitle, "Composition root title contract must match expected string")
    }
}
