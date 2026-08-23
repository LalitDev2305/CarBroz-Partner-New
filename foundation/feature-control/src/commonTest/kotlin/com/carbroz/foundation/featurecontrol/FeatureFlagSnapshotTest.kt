package com.carbroz.foundation.featurecontrol

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagSnapshotTest {
    @Test
    fun `missing flag resolves to unknown`() {
        val known = FeatureFlag("checkout.v2")
        val missing = FeatureFlag("maps.new")
        val snapshot = FeatureFlagSnapshot(mapOf(known to FeatureFlagState.Enabled))

        assertEquals(FeatureFlagState.Enabled, snapshot.state(known))
        assertEquals(FeatureFlagState.Unknown, snapshot.state(missing))
    }
}
