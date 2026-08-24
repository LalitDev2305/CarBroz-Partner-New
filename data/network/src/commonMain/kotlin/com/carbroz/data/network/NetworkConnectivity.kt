package com.carbroz.data.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Network-facing connectivity capability.
 *
 * Platform reachability implementations belong in application/platform composition; data:network
 * only consumes the normalized state and never owns Android/iOS connectivity APIs.
 */
fun interface NetworkConnectivityProvider {
    fun connectivity(): NetworkConnectivity
}

/** Read-only observable connectivity for long-lived runtime consumers such as sync activation. */
interface NetworkConnectivityObserver : NetworkConnectivityProvider {
    val state: StateFlow<NetworkConnectivity>

    override fun connectivity(): NetworkConnectivity = state.value
}

enum class NetworkConnectivity {
    ONLINE,
    OFFLINE,
    UNKNOWN,
}

/** Unknown is deliberately fail-open: lack of a reachability signal must not block transport. */
data object UnknownNetworkConnectivityProvider : NetworkConnectivityProvider {
    override fun connectivity(): NetworkConnectivity = NetworkConnectivity.UNKNOWN
}
