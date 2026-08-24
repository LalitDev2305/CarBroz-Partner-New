package com.carbroz.data.network

/**
 * Network-facing connectivity capability.
 *
 * Platform reachability implementations belong in application/platform composition; data:network
 * only consumes the normalized state and never owns Android/iOS connectivity APIs.
 */
fun interface NetworkConnectivityProvider {
    fun connectivity(): NetworkConnectivity
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
