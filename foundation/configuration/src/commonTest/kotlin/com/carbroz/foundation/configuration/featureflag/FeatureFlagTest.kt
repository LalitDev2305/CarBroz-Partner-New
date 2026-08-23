package com.carbroz.foundation.configuration.featureflag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FeatureFlagTest {
    @Test
    fun `valid feature flag key is preserved`() {
        assertEquals("booking.new_checkout", FeatureFlag("booking.new_checkout").key)
    }

    @Test
    fun `malformed feature flag keys are rejected`() {
        assertFailsWith<IllegalArgumentException> { FeatureFlag("AB") }
        assertFailsWith<IllegalArgumentException> { FeatureFlag("Booking.New") }
        assertFailsWith<IllegalArgumentException> { FeatureFlag("1booking") }
        assertFailsWith<IllegalArgumentException> { FeatureFlag("a".repeat(65)) }
    }

    @Test
    fun `provider can explicitly report unknown state`() {
        val provider = FeatureFlagProvider { FeatureFlagState.Unknown }
        assertEquals(FeatureFlagState.Unknown, provider.state(FeatureFlag("booking.new_checkout")))
    }
}
