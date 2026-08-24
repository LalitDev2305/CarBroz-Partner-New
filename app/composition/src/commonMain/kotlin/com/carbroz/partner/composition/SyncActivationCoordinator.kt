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

/**
 * Application-level activation policy for foreground sync.
 *
 * Sync semantics remain owned by data:sync; this coordinator only translates application lifecycle
 * and semantic connectivity transitions into sync triggers. Failures are isolated so long-lived
 * observation survives an individual synchronization failure.
 */
class SyncActivationCoordinator(
    private val lifecycle: AppLifecycle,
    private val connectivity: NetworkConnectivityObserver,
    private val syncCoordinator: SyncCoordinator,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        launch {
            lifecycle.state.collect { state ->
                if (state == AppLifecycleState.Foreground) {
                    synchronizeSafely(SyncTrigger.FOREGROUND)
                }
            }
        }

        launch {
            var previous = connectivity.state.value
            connectivity.state.collect { current ->
                val restored = current == NetworkConnectivity.ONLINE && previous != NetworkConnectivity.ONLINE
                previous = current
                if (restored) {
                    synchronizeSafely(SyncTrigger.CONNECTIVITY_RESTORED)
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
            // Activation observation is long-lived; one failed attempt must not terminate it.
        }
    }
}
