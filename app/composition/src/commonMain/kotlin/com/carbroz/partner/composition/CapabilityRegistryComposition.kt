package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRegistry

/** Process-composition helper that assembles the platform-provided capability registry. */
internal fun createCapabilityRegistry(
    providers: List<CapabilityProvider>,
): CapabilityRegistry = CapabilityRegistry.builder()
    .apply { providers.forEach(::register) }
    .build()
