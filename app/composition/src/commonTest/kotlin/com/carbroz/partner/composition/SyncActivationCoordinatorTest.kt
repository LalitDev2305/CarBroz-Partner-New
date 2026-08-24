package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkConnectivity
import com.carbroz.data.network.NetworkConnectivityObserver
import com.carbroz.data.sync.SyncCoordinator
import com.carbroz.data.sync.SyncReport
import com.carbroz.data.sync.SyncTrigger
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SyncActivationCoordinatorTest {
    @Test
    fun foregroundAndConnectivityTransitionsTriggerSyncWithoutDuplicateOnlineEmission() = runTest {
        val lifecycle = FakeLifecycle()
        val connectivity = FakeConnectivity()
        val sync = RecordingSyncCoordinator()
        val coordinator = SyncActivationCoordinator(lifecycle, connectivity, sync)
        val job = coordinator.start(this)
        runCurrent()

        lifecycle.mutable.value = AppLifecycleState.Foreground
        advanceUntilIdle()
        connectivity.mutable.value = NetworkConnectivity.ONLINE
        advanceUntilIdle()
        connectivity.mutable.value = NetworkConnectivity.ONLINE
        advanceUntilIdle()

        assertEquals(
            listOf(SyncTrigger.FOREGROUND, SyncTrigger.CONNECTIVITY_RESTORED),
            sync.triggers,
        )
        job.cancel()
    }

    @Test
    fun activationSurvivesIndividualSyncFailure() = runTest {
        val lifecycle = FakeLifecycle()
        val connectivity = FakeConnectivity()
        val sync = RecordingSyncCoordinator(failFirst = true)
        val coordinator = SyncActivationCoordinator(lifecycle, connectivity, sync)
        val job = coordinator.start(this)
        runCurrent()

        lifecycle.mutable.value = AppLifecycleState.Foreground
        advanceUntilIdle()
        lifecycle.mutable.value = AppLifecycleState.Background
        advanceUntilIdle()
        lifecycle.mutable.value = AppLifecycleState.Foreground
        advanceUntilIdle()

        assertEquals(listOf(SyncTrigger.FOREGROUND, SyncTrigger.FOREGROUND), sync.triggers)
        job.cancel()
    }

    @Test
    fun manualRequestUsesManualTrigger() = runTest {
        val sync = RecordingSyncCoordinator()
        val coordinator = SyncActivationCoordinator(FakeLifecycle(), FakeConnectivity(), sync)

        coordinator.requestManualSync()

        assertEquals(listOf(SyncTrigger.MANUAL), sync.triggers)
    }

    private class FakeLifecycle : AppLifecycle {
        val mutable = MutableStateFlow(AppLifecycleState.Unknown)
        override val state: StateFlow<AppLifecycleState> = mutable
    }

    private class FakeConnectivity : NetworkConnectivityObserver {
        val mutable = MutableStateFlow(NetworkConnectivity.OFFLINE)
        override val state: StateFlow<NetworkConnectivity> = mutable
    }

    private class RecordingSyncCoordinator(
        private val failFirst: Boolean = false,
    ) : SyncCoordinator {
        val triggers = mutableListOf<SyncTrigger>()

        override suspend fun synchronize(trigger: SyncTrigger): SyncReport {
            triggers += trigger
            if (failFirst && triggers.size == 1) error("expected test failure")
            return SyncReport(0, 0, 0, 0, 0)
        }
    }
}
