package com.carbroz.foundation.capabilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CapabilityRegistryTest {
    @Test
    fun missingProviderIsExplicitlyUnsupported() = runTest {
        val registry = CapabilityRegistry.builder().build()

        val result = registry.execute(GenericCapabilityRequest(CapabilityKind.CAMERA, "capture"))

        assertIs<CapabilityResult.Unsupported>(result)
        assertIs<CapabilityAvailability.Unsupported>(registry.availability(CapabilityKind.CAMERA))
    }

    @Test
    fun permissionRequiredProviderDoesNotExecute() = runTest {
        val provider = FakeProvider(
            kind = CapabilityKind.CAMERA,
            initialAvailability = CapabilityAvailability.PermissionRequired("camera permission required"),
        )
        val registry = CapabilityRegistry.builder().register(provider).build()

        val result = registry.execute(GenericCapabilityRequest(CapabilityKind.CAMERA, "capture"))

        assertIs<CapabilityResult.PermissionRequired>(result)
        assertEquals(0, provider.executionCount)
    }

    @Test
    fun restrictedProviderDoesNotExecute() = runTest {
        val provider = FakeProvider(
            kind = CapabilityKind.LOCATION,
            initialAvailability = CapabilityAvailability.Restricted("permission denied"),
        )
        val registry = CapabilityRegistry.builder().register(provider).build()

        val result = registry.execute(GenericCapabilityRequest(CapabilityKind.LOCATION, "current"))

        assertIs<CapabilityResult.Restricted>(result)
        assertEquals(0, provider.executionCount)
    }

    @Test
    fun availableProviderExecutesThroughCanonicalRegistry() = runTest {
        val provider = FakeProvider(CapabilityKind.SHARING, CapabilityAvailability.Available)
        val registry = CapabilityRegistry.builder().register(provider).build()

        val result = registry.execute(GenericCapabilityRequest(CapabilityKind.SHARING, "share"))

        assertIs<CapabilityResult.Success>(result)
        assertEquals(1, provider.executionCount)
    }

    @Test
    fun duplicateProviderRegistrationFails() {
        val provider = FakeProvider(CapabilityKind.MEDIA, CapabilityAvailability.Available)
        val builder = CapabilityRegistry.builder().register(provider)

        assertFailsWith<IllegalArgumentException> { builder.register(provider) }
    }

    @Test
    fun coordinatesRejectNonFiniteAndOutOfRangeValues() {
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, 181.0) }
    }

    private class FakeProvider(
        override val kind: CapabilityKind,
        initialAvailability: CapabilityAvailability,
    ) : CapabilityProvider {
        private val mutableAvailability = MutableStateFlow(initialAvailability)
        override val availability: StateFlow<CapabilityAvailability> = mutableAvailability
        var executionCount: Int = 0
            private set

        override suspend fun execute(request: CapabilityRequest): CapabilityResult {
            require(request.kind == kind)
            executionCount += 1
            return CapabilityResult.Success()
        }
    }
}
