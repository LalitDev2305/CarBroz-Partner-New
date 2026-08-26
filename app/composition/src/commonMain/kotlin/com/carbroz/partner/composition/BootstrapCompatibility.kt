package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkDataSource

/** Temporary source-compatibility alias while DI composition is migrated to the generic name. */
@Deprecated("Use BootstrapDestinationStore", ReplaceWith("BootstrapDestinationStore"))
typealias BootstrapRouteStore = BootstrapDestinationStore

/** Keeps existing named-argument DI source compiling while removing route semantics from runtime state. */
@Suppress("FunctionName")
internal fun BootstrapConfigurationStartupTask(
    network: NetworkDataSource,
    routes: BootstrapDestinationStore,
    compatibilityBridge: Unit = Unit,
): BootstrapConfigurationStartupTask {
    compatibilityBridge.hashCode()
    return BootstrapConfigurationStartupTask(network = network, destinations = routes)
}
