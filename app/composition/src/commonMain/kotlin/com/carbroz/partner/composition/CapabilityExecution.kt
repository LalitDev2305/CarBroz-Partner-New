package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.runtime.action.PreparedAction

/** Application integration boundary from prepared dynamic capability actions to the canonical registry. */
class CapabilityActionExecutor(
    private val registry: CapabilityRegistry,
) {
    suspend fun execute(action: PreparedAction.Capability): CapabilityResult =
        registry.execute(action.request)
}
