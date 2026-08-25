package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.capabilities.StaticCapabilityProvider
import com.carbroz.runtime.action.PreparedAction

/** Application integration boundary from prepared dynamic capability actions to the canonical registry. */
class CapabilityActionExecutor(
    private val registry: CapabilityRegistry,
) {
    suspend fun execute(action: PreparedAction.Capability): CapabilityResult =
        registry.execute(action.request)
}

internal fun createCapabilityRegistry(
    platformProviders: List<CapabilityProvider>,
): CapabilityRegistry {
    val builder = CapabilityRegistry.builder().registerAll(platformProviders)
    CapabilityKind.entries.forEach { kind ->
        builder.registerIfAbsent(
            StaticCapabilityProvider(
                kind = kind,
                state = CapabilityAvailability.Unsupported("$kind is not supported by this platform host"),
            ),
        )
    }
    return builder.build()
}
