package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.StaticCapabilityProvider

/** Process-wide platform capability registry assembly; execution belongs to feature:dynamic. */
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
