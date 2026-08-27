package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CapabilityExecutionTest {
    @Test
    fun registryFillsMissingPlatformCapabilitiesWithExplicitUnsupportedProviders() {
        val registry = createCapabilityRegistry(emptyList())

        assertEquals(CapabilityKind.entries.size, registry.size)
        assertIs<CapabilityAvailability.Unsupported>(registry.availability(CapabilityKind.CAMERA))
    }
}
