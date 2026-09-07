package com.carbroz.feature.splash

import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.ApplicationRuntimeState
import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SplashStoreTest {
    @Test
    fun `foreground starts runtime once and duplicate foreground does not duplicate startup`() = runTest {
        val runtime = FakeRuntime()
        val store = SplashStore(runtime, backgroundScope)

        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()
        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()

        assertEquals(1, runtime.startCalls)
        store.close()
    }

    @Test
    fun `ready runtime publishes clean state and one navigation effect per attempt`() = runTest {
        val runtime = FakeRuntime()
        val store = SplashStore(runtime, backgroundScope)
        val effects = mutableListOf<SplashEffect>()
        val collector = backgroundScope.launch { store.effects.collect(effects::add) }
        runCurrent()

        runtime.publish(ApplicationRuntimeState.Ready(TestPayload, emptyList(), 1u))
        runCurrent()
        runtime.publish(ApplicationRuntimeState.Ready(TestPayload, emptyList(), 1u))
        runCurrent()

        assertEquals(SplashState.Ready, store.state.value)
        assertEquals(listOf(SplashEffect.Navigate(TestPayload)), effects)
        collector.cancel()
        store.close()
    }

    @Test
    fun `required update maps to state and update intent emits URI effect`() = runTest {
        val runtime = FakeRuntime()
        val store = SplashStore(runtime, backgroundScope)
        val effects = mutableListOf<SplashEffect>()
        val collector = backgroundScope.launch { store.effects.collect(effects::add) }
        runCurrent()

        runtime.publish(
            ApplicationRuntimeState.Blocked(
                StartupBlocker.RequiredUpdate("Update", "Required", "https://example.com/update"),
                1u,
            ),
        )
        runCurrent()
        store.dispatch(SplashIntent.UpdateClicked)
        runCurrent()

        assertIs<SplashState.RequiredUpdate>(store.state.value)
        assertEquals(listOf(SplashEffect.OpenUpdateUri("https://example.com/update")), effects)
        collector.cancel()
        store.close()
    }

    @Test
    fun `maintenance retry dispatches runtime retry only when foreground`() = runTest {
        val runtime = FakeRuntime()
        val store = SplashStore(runtime, backgroundScope)
        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()
        runtime.publish(
            ApplicationRuntimeState.Blocked(
                StartupBlocker.Maintenance("Maintenance", "Try later", retryable = true),
                1u,
            ),
        )
        runCurrent()

        store.dispatch(SplashIntent.RetryClicked)
        runCurrent()

        assertEquals(1, runtime.retryCalls)
        assertIs<SplashState.Maintenance>(store.state.value)
        store.close()
    }

    @Test
    fun `runtime failure is mapped to presentation error without leaking failure type`() = runTest {
        val runtime = FakeRuntime()
        val store = SplashStore(runtime, backgroundScope)
        runtime.publish(
            ApplicationRuntimeState.Failed(
                taskId = "partner.bootstrap",
                failure = StartupFailure.Expected("bootstrap_timeout", recoverable = true),
                attempt = 1u,
            ),
        )
        runCurrent()

        val error = assertIs<SplashState.Error>(store.state.value)
        assertEquals(true, error.retryEnabled)
        store.close()
    }

    private class FakeRuntime : ApplicationRuntime {
        private val mutableState = MutableStateFlow<ApplicationRuntimeState>(ApplicationRuntimeState.Idle)
        override val state: StateFlow<ApplicationRuntimeState> = mutableState
        var startCalls: Int = 0
        var retryCalls: Int = 0

        override suspend fun start(): ApplicationRuntimeState {
            startCalls += 1
            return mutableState.value
        }

        override suspend fun retry(): ApplicationRuntimeState {
            retryCalls += 1
            return mutableState.value
        }

        fun publish(state: ApplicationRuntimeState) {
            mutableState.value = state
        }
    }

    private data object TestPayload : StartupPayload
}
