package com.carbroz.foundation.capabilities

import kotlinx.coroutines.flow.StateFlow

interface CapabilityProvider {
    val kind: CapabilityKind
    val availability: StateFlow<CapabilityAvailability>

    /** Implementations must reject requests for a different [kind]. */
    suspend fun execute(request: CapabilityRequest): CapabilityResult
}

class CapabilityRegistry internal constructor(
    private val providers: Map<CapabilityKind, CapabilityProvider>,
) {
    val size: Int get() = providers.size

    fun find(kind: CapabilityKind): CapabilityProvider? = providers[kind]

    fun availability(kind: CapabilityKind): CapabilityAvailability =
        providers[kind]?.availability?.value
            ?: CapabilityAvailability.Unsupported("No provider registered for $kind")

    suspend fun execute(request: CapabilityRequest): CapabilityResult {
        val provider = providers[request.kind]
            ?: return CapabilityResult.Unsupported("No provider registered for ${request.kind}")
        return when (val state = provider.availability.value) {
            CapabilityAvailability.Available -> provider.execute(request)
            is CapabilityAvailability.Restricted -> CapabilityResult.Restricted(state.reason)
            is CapabilityAvailability.Unavailable -> CapabilityResult.Unavailable(state.reason)
            is CapabilityAvailability.Unsupported -> CapabilityResult.Unsupported(state.reason)
        }
    }

    companion object {
        fun builder(): CapabilityRegistryBuilder = CapabilityRegistryBuilder()
    }
}

class CapabilityRegistryBuilder {
    private val providers = linkedMapOf<CapabilityKind, CapabilityProvider>()

    fun register(provider: CapabilityProvider): CapabilityRegistryBuilder = apply {
        require(provider.kind !in providers) { "Duplicate capability provider for ${provider.kind}" }
        providers[provider.kind] = provider
    }

    fun registerAll(values: Iterable<CapabilityProvider>): CapabilityRegistryBuilder = apply {
        values.forEach(::register)
    }

    fun build(): CapabilityRegistry = CapabilityRegistry(providers.toMap())
}
