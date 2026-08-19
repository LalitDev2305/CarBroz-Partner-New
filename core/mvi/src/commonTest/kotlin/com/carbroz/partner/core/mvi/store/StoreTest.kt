package com.carbroz.partner.core.mvi.store

import com.carbroz.partner.core.mvi.store.Store
import com.carbroz.partner.core.mvi.store.createStore
import com.carbroz.partner.core.mvi.store.IntentScope

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {

    private sealed interface TestState {
        data class Count(val value: Int) : TestState
    }

    private sealed interface TestIntent {
        data class Increment(val amount: Int) : TestIntent
        data object TriggerEffect : TestIntent
        data object ThrowDefect : TestIntent
        data object ThrowCancellation : TestIntent
    }

    private sealed interface TestEffect {
        data class ShowMessage(val text: String) : TestEffect
    }

    private class TestLogger : StructuredLogger {
        val events = mutableListOf<LogEvent>()

        override fun withSource(sourceClass: String, defaultTraceContext: TraceContext?): BoundLogger {
            return object : BoundLogger {
                override fun log(
                    level: LogLevel,
                    category: LogCategory,
                    sourceFunction: String,
                    event: String,
                    message: String,
                    attributes: Map<String, LogAttribute>,
                    traceContext: TraceContext?,
                    durationMs: Long?,
                    throwable: Throwable?
                ) {
                    events.add(
                        LogEvent(
                            timestampMs = 0L,
                            level = level,
                            category = category,
                            sourceClass = sourceClass,
                            sourceFunction = sourceFunction,
                            event = event,
                            message = message,
                            attributes = emptyMap(),
                            traceContext = traceContext,
                            durationMs = durationMs,
                            errorInfo = null
                        )
                    )
                }

                override fun info(sourceFunction: String, category: LogCategory, event: String, message: String, attributes: Map<String, LogAttribute>, traceContext: TraceContext?) {
                    log(LogLevel.INFO, category, sourceFunction, event, message, attributes, traceContext)
                }

                override fun debug(sourceFunction: String, category: LogCategory, event: String, message: String, attributes: Map<String, LogAttribute>, traceContext: TraceContext?) {
                    log(LogLevel.DEBUG, category, sourceFunction, event, message, attributes, traceContext)
                }

                override fun error(sourceFunction: String, category: LogCategory, event: String, message: String, throwable: Throwable?, attributes: Map<String, LogAttribute>, traceContext: TraceContext?) {
                    log(LogLevel.ERROR, category, sourceFunction, event, message, attributes, traceContext, throwable = throwable)
                }
            }
        }

        override fun isLevelEnabled(level: LogLevel): Boolean = true
    }

    @Test
    fun verifyInitialStateExposedCorrectly() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(10),
            storeId = "TestStore"
        ) { }

        assertEquals(TestState.Count(10), store.state.value)
    }

    @Test
    fun verifySequentialIntentsAndAtomicStateUpdates() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore"
        ) { intent ->
            when (intent) {
                is TestIntent.Increment -> updateState { current ->
                    val c = current as TestState.Count
                    TestState.Count(c.value + intent.amount)
                }
                else -> {}
            }
        }

        store.dispatch(TestIntent.Increment(5))
        store.dispatch(TestIntent.Increment(3))

        assertEquals(TestState.Count(8), store.state.value)
    }

    @Test
    fun verifyEffectSingleConsumerDeliveryAndNoReplay() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore"
        ) { intent ->
            if (intent is TestIntent.TriggerEffect) {
                emitEffect(TestEffect.ShowMessage("Hello"))
            }
        }

        var receivedEffect: TestEffect? = null
        val job = testScope.launch {
            receivedEffect = store.effects.first()
        }

        store.dispatch(TestIntent.TriggerEffect)

        assertEquals(TestEffect.ShowMessage("Hello"), receivedEffect)
        job.cancel()

        var lateEffect: TestEffect? = null
        val lateJob = testScope.launch {
            lateEffect = store.effects.firstOrNull()
        }
        lateJob.cancel()
        assertNull(lateEffect)
    }

    @Test
    fun verifyParentScopeCancellationDisposesStore() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore",
            logger = logger
        ) { }

        testScope.cancel()

        assertTrue(logger.events.any { it.event == "STORE_SCOPE_COMPLETED" })
    }

    @Test
    fun verifyDispatchFailsOrSuspendsAfterScopeCancellation() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore"
        ) { }

        testScope.cancel()

        assertFailsWith<Throwable> {
            store.dispatch(TestIntent.Increment(1))
        }
    }

    @Test
    fun verifyCancellationExceptionIsPreservedAndRethrown() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore",
            logger = logger
        ) { intent ->
            if (intent is TestIntent.ThrowCancellation) {
                throw CancellationException("Simulated cancellation")
            }
        }

        store.dispatch(TestIntent.ThrowCancellation)

        assertTrue(logger.events.none { it.event == "INTENT_PROCESSING_FAILED" })
    }


    @Test
    fun verifyProgrammerDefectIsLoggedAndHandled() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore",
            logger = logger
        ) { intent ->
            if (intent is TestIntent.ThrowDefect) {
                throw IllegalStateException("Simulated defect")
            }
        }

        testScope.launch {
            store.dispatch(TestIntent.ThrowDefect)
        }

        assertTrue(logger.events.any { it.event == "INTENT_PROCESSING_FAILED" })

        assertFailsWith<Throwable> {
            store.dispatch(TestIntent.Increment(1))
        }
    }




    @Test
    fun verifyObservabilityLogsWithoutPayloadDumping() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore",
            logger = logger
        ) { intent ->
            if (intent is TestIntent.Increment) {
                updateState { TestState.Count(100) }
            }
        }

        store.dispatch(TestIntent.Increment(1))

        assertTrue(logger.events.all { !it.message.contains("Count(100)") })
        assertTrue(logger.events.all { !it.message.contains("Increment(1)") })
    }

    @Test
    fun verifyEffectEmittedWithoutActiveCollectorDoesNotSuspendProcessor() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore"
        ) { intent ->
            if (intent is TestIntent.TriggerEffect) {
                emitEffect(TestEffect.ShowMessage("BufferedEffect"))
                updateState { TestState.Count(99) }
            }
        }

        // Dispatch intent without any active effect collector
        store.dispatch(TestIntent.TriggerEffect)

        // State update must complete successfully despite absence of effect collector
        assertEquals(TestState.Count(99), store.state.value)

        // Late collector receives the buffered effect
        val received = store.effects.first()
        assertEquals(TestEffect.ShowMessage("BufferedEffect"), received)
    }

    @Test
    fun verifyBufferedEffectsRetainFifoOrdering() = runTest {
        val testScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState.Count(0),
            storeId = "TestStore"
        ) { intent ->
            if (intent is TestIntent.TriggerEffect) {
                emitEffect(TestEffect.ShowMessage("Msg 1"))
                emitEffect(TestEffect.ShowMessage("Msg 2"))
                emitEffect(TestEffect.ShowMessage("Msg 3"))
            }
        }

        store.dispatch(TestIntent.TriggerEffect)

        val collected = mutableListOf<TestEffect>()
        val job = testScope.launch {
            store.effects.collect { effect: TestEffect ->
                collected.add(effect)
            }
        }

        val expected: List<TestEffect> = listOf(
            TestEffect.ShowMessage("Msg 1"),
            TestEffect.ShowMessage("Msg 2"),
            TestEffect.ShowMessage("Msg 3")
        )
        assertEquals(expected, collected)
        job.cancel()
    }
}
