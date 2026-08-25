package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRequest
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.runtime.action.PreparedAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
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

    @Test
    fun preparedActionExecutesThroughRegisteredPlatformProvider() = runTest {
        val provider = RecordingProvider()
        val executor = CapabilityActionExecutor(createCapabilityRegistry(listOf(provider)))
        val request = GenericCapabilityRequest(CapabilityKind.SHARING, "share")

        val result = executor.execute(PreparedAction.Capability(request))

        assertIs<CapabilityResult.Success>(result)
        assertEquals(1, provider.calls)
    }

    private class RecordingProvider : CapabilityProvider {
        override val kind: CapabilityKind = CapabilityKind.SHARING
        override val availability: StateFlow<CapabilityAvailability> =
            MutableStateFlow(CapabilityAvailability.Available)
        var calls: Int = 0
            private set

        override suspend fun execute(request: CapabilityRequest): CapabilityResult {
            require(request.kind == kind)
            calls += 1
            return CapabilityResult.Success()
        }
    }
}
