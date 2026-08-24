package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkConnectivity
import com.carbroz.data.network.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.Koin
import org.koin.mp.KoinPlatform

/**
 * Application-owned semantic connectivity state.
 *
 * Platform hosts translate their native reachability callbacks into [NetworkConnectivity]; no
 * platform connectivity APIs cross into common data/runtime modules.
 */
class ApplicationConnectivity(
    initialState: NetworkConnectivity = NetworkConnectivity.UNKNOWN,
) : NetworkConnectivityObserver {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<NetworkConnectivity> = mutableState.asStateFlow()

    fun update(connectivity: NetworkConnectivity) {
        mutableState.value = connectivity
    }
}

/** Thin host bridge for platform reachability callbacks. */
object AppConnectivityBridge {
    val connectivity: NetworkConnectivityObserver
        get() = koin().get()

    fun moveOnline() = controller().update(NetworkConnectivity.ONLINE)
    fun moveOffline() = controller().update(NetworkConnectivity.OFFLINE)
    fun moveToUnknown() = controller().update(NetworkConnectivity.UNKNOWN)

    private fun controller(): ApplicationConnectivity = koin().get()

    private fun koin(): Koin = KoinPlatform.getKoinOrNull()
        ?: error("CarBroz dependency injection must be initialized before connectivity events are forwarded.")
}
