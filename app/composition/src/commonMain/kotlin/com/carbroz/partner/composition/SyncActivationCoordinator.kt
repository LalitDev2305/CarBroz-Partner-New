package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkConnectivity
import com.carbroz.data.network.NetworkConnectivityObserver
import com.carbroz.data.sync.SyncCoordinator
import com.carbroz.data.sync.SyncTrigger
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Offline writes are operation-specific; automatic sync stays disabled until product policy enables it. */
data class SyncActivationPolicy(
    val foregroundEnabled: Boolean = false,
    val connectivityRestoredEnabled: Boolean = false,
)

internal class SyncActivationCoordinator(
    private val lifecycle: AppLifecycle,
    private val connectivity: NetworkConnectivityObserver,
    private val syncCoordinator: SyncCoordinator,
    private val policy: SyncActivationPolicy = SyncActivationPolicy(),
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        if (policy.foregroundEnabled) {
            launch {
                lifecycle.state.collect { state ->
                    if (state == AppLifecycleState.Foreground) synchronizeSafely(SyncTrigger.FOREGROUND)
                }
            }
        }

        if (policy.connectivityRestoredEnabled) {
            launch {
                var previous = connectivity.state.value
                connectivity.state.collect { current ->
                    val restored = current == NetworkConnectivity.ONLINE && previous != NetworkConnectivity.ONLINE
                    previous = current
                    if (restored) synchronizeSafely(SyncTrigger.CONNECTIVITY_RESTORED)
                }
            }
        }
    }

    suspend fun requestManualSync() {
        syncCoordinator.synchronize(SyncTrigger.MANUAL)
    }

    private suspend fun synchronizeSafely(trigger: SyncTrigger) {
        try {
            syncCoordinator.synchronize(trigger)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Long-lived activation observation must survive one failed synchronization attempt.
        }
    }
}
