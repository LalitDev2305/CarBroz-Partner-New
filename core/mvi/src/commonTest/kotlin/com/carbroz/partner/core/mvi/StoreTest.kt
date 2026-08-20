package com.carbroz.partner.core.mvi

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {

    private data class TestState(val count: Int = 0, val text: String = "")
    private sealed interface TestIntent {
        data class Increment(val amount: Int) : TestIntent
        data class SetText(val text: String) : TestIntent
        data class SaturateEffects(val count: Int) : TestIntent
        data object FailingIntent : TestIntent
        data object CancellingIntent : TestIntent
        data object EmitEffectIntent : TestIntent
    }
    private sealed interface TestEffect {
        data class ShowToast(val id: Int) : TestEffect
    }

    private class TestLogger : StructuredLogger {
        val events = mutableListOf<LogEvent>()

        override fun isLevelEnabled(level: LogLevel): Boolean = true

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
                            timestampMs = 1000L,
                            level = level,
                            category = category,
                            sourceClass = sourceClass,
                            sourceFunction = sourceFunction,
                            event = event,
                            message = message,
                            traceContext = traceContext,
                            errorInfo = throwable?.let {
                                com.carbroz.partner.core.observability.model.ErrorInfo(
                                    type = it::class.simpleName ?: "Throwable",
                                    message = it.message
                                )
                            }
                        )
                    )
                }
            }
        }
    }

    @Test
    fun verifyInitialStateExposedCorrectlyAndReadOnly() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState(count = 10, text = "init"),
            storeId = "TestStore",
            logger = TestLogger(),
            processor = {}
        )

        assertEquals(TestState(count = 10, text = "init"), store.state.value)
    }

    @Test
    fun verifyStateUpdatesAreDeterministicAndUseLatestState() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState(count = 0),
            storeId = "TestStore",
            logger = TestLogger(),
            processor = { intent ->
                when (intent) {
                    is TestIntent.Increment -> updateState { current ->
                        assertEquals(currentState, current)
                        current.copy(count = current.count + intent.amount)
                    }
                    else -> {}
                }
            }
        )

        store.dispatch(TestIntent.Increment(5))
        store.dispatch(TestIntent.Increment(3))

        assertEquals(8, store.state.value.count)
    }

    @Test
    fun verifyMultipleIntentsProcessedFifoAndNeverConcurrently() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val executionOrder = mutableListOf<String>()
        var inFlight = 0

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState(),
            storeId = "TestStore",
            logger = TestLogger(),
            processor = { intent ->
                assertEquals(1, ++inFlight, "Processor executed concurrently!")
                when (intent) {
                    is TestIntent.SetText -> {
                        executionOrder.add("start_${intent.text}")
                        delay(10)
                        executionOrder.add("end_${intent.text}")
                    }
                    else -> {}
                }
                inFlight--
            }
        )

        testScope.launch { store.dispatch(TestIntent.SetText("A")) }
        testScope.launch { store.dispatch(TestIntent.SetText("B")) }
        testScope.launch { store.dispatch(TestIntent.SetText("C")) }

        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf("start_A", "end_A", "start_B", "end_B", "start_C", "end_C"),
            executionOrder
        )
    }

    @Test
    fun verifyEffectsDeliveredToActiveCollector() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState(),
            storeId = "TestStore",
            logger = TestLogger(),
            processor = { intent ->
                if (intent is TestIntent.EmitEffectIntent) {
                    emitEffect(TestEffect.ShowToast(1))
                }
            }
        )

        val collected = mutableListOf<TestEffect>()
        val collectorJob = testScope.launch {
            store.effects.toList(collected)
        }

        store.dispatch(TestIntent.EmitEffectIntent)
        testScheduler.advanceUntilIdle()

        assertEquals(1, collected.size)
        assertEquals(TestEffect.ShowToast(1), collected[0])

        collectorJob.cancel()
    }

    @Test
    fun verifyQueuedEffectEmittedWithoutCollectorDeliveredExactlyOnceAndNotReplayed() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState(),
            storeId = "TestStore",
            logger = TestLogger(),
            processor = { intent ->
                if (intent is TestIntent.EmitEffectIntent) {
                    emitEffect(TestEffect.ShowToast(1))
                    emitEffect(TestEffect.ShowToast(2))
                }
            }
        )

        // Emit effects before any collector is attached
        store.dispatch(TestIntent.EmitEffectIntent)

        val firstCollectorEffects = mutableListOf<TestEffect>()
        val firstCollectorJob = testScope.launch {
            store.effects.toList(firstCollectorEffects)
        }
        testScheduler.advanceUntilIdle()

        assertEquals(2, firstCollectorEffects.size)
        assertEquals(TestEffect.ShowToast(1), firstCollectorEffects[0])
        assertEquals(TestEffect.ShowToast(2), firstCollectorEffects[1])

        firstCollectorJob.cancel()

        // Second (late) collector must receive NOTHING (no replay)
        val secondCollectorEffects = mutableListOf<TestEffect>()
        val secondCollectorJob = testScope.launch {
            store.effects.toList(secondCollectorEffects)
        }
        testScheduler.advanceUntilIdle()

        assertEquals(0, secondCollectorEffects.size)

        secondCollectorJob.cancel()
    }

    @Test
    fun verifyEffectBufferOverflowDropsNewestAndDoesNotBlockProcessor() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = TestState(),
            storeId = "TestStore",
            logger = logger,
            processor = { intent ->
                if (intent is TestIntent.SaturateEffects) {
                    for (i in 1..intent.count) {
                        emitEffect(TestEffect.ShowToast(i))
                    }
                    updateState { it.copy(count = intent.count) }
                }
            }
        )

        // Saturate buffer with 70 effects without a collector (buffer capacity = 64)
        store.dispatch(TestIntent.SaturateEffects(70))
        testScheduler.advanceUntilIdle()

        // Verify state processing completed normally without deadlock
        assertEquals(70, store.state.value.count)

        // Verify overflow log event exists with no effect payload in message
        val overflowLogs = logger.events.filter { it.event == "EFFECT_DROPPED_BUFFER_FULL" }
        assertTrue(overflowLogs.isNotEmpty(), "Overflow warning should be logged")
        assertEquals(LogLevel.WARN, overflowLogs[0].level)
        assertFalse(overflowLogs[0].message.contains("ShowToast"))

        // Collect retained effects: exactly 64 items preserved in FIFO order (1..64)
        val collected = mutableListOf<TestEffect>()
        val collectorJob = testScope.launch {
            store.effects.toList(collected)
        }
        testScheduler.advanceUntilIdle()

        assertEquals(64, collected.size)
        assertEquals(TestEffect.ShowToast(1), collected[0])
        assertEquals(TestEffect.ShowToast(64), collected[63])

        collectorJob.cancel()
    }

    @Test
    fun verifyParentCancellationTerminatesStoreAndRejectsDispatch() = runTest {
        val job = SupervisorJob()
        val customScope = CoroutineScope(job + UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = customScope,
            initialState = TestState(),
            storeId = "TestStore",
            logger = logger,
            processor = {}
        )

        customScope.cancel()
        testScheduler.advanceUntilIdle()

        // STORE_TERMINATED must be logged exactly once
        val terminatedLogs = logger.events.filter { it.event == "STORE_TERMINATED" }
        assertEquals(1, terminatedLogs.size, "STORE_TERMINATED must be emitted exactly once")
        assertEquals("onProcessorCompletion", terminatedLogs[0].sourceFunction)

        assertFailsWith<CancellationException> {
            store.dispatch(TestIntent.Increment(1))
        }
    }

    @Test
    fun verifyDispatchFailsAfterProcessorCancellationWhileParentScopeRemainsActive() = runTest {
        val parentScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = parentScope,
            initialState = TestState(),
            storeId = "TestStore",
            logger = logger,
            processor = { intent ->
                if (intent is TestIntent.CancellingIntent) {
                    throw CancellationException("Processor cancelled")
                }
            }
        )

        store.dispatch(TestIntent.CancellingIntent)
        testScheduler.advanceUntilIdle()

        // Parent scope remains active because caller provided SupervisorJob
        assertTrue(parentScope.isActive, "Caller-provided supervised parent scope should remain active")

        // STORE_TERMINATED must be logged exactly once
        val terminatedLogs = logger.events.filter { it.event == "STORE_TERMINATED" }
        assertEquals(1, terminatedLogs.size, "STORE_TERMINATED must be emitted exactly once")

        // CancellationException must NOT be logged as INTENT_PROCESSING_FAILED
        val failedLogs = logger.events.filter { it.event == "INTENT_PROCESSING_FAILED" }
        assertEquals(0, failedLogs.size)

        // Subsequent dispatch MUST fail and NOT hang
        assertFailsWith<CancellationException> {
            store.dispatch(TestIntent.Increment(1))
        }

        parentScope.cancel()
    }

    @Test
    fun verifyUnexpectedDefectTerminatesStoreAndIsLoggedWithoutPayloadDumping() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()
        val initialState = TestState(count = 0, text = "sensitive_state_secret")

        val store = createStore<TestState, TestIntent, TestEffect>(
            scope = testScope,
            initialState = initialState,
            storeId = "TestStore",
            logger = logger,
            processor = { intent ->
                if (intent is TestIntent.FailingIntent) {
                    throw IllegalStateException("Database corrupted")
                } else if (intent is TestIntent.Increment) {
                    updateState { it.copy(count = it.count + intent.amount) }
                }
            }
        )

        store.dispatch(TestIntent.FailingIntent)
        testScheduler.advanceUntilIdle()

        // Verify defect logged exactly once
        val failedLogs = logger.events.filter { it.event == "INTENT_PROCESSING_FAILED" }
        assertEquals(1, failedLogs.size)
        assertEquals("Uncaught error during intent processing", failedLogs[0].message)
        assertEquals("IllegalStateException", failedLogs[0].errorInfo?.type)

        // STORE_TERMINATED must be logged exactly once
        val terminatedLogs = logger.events.filter { it.event == "STORE_TERMINATED" }
        assertEquals(1, terminatedLogs.size, "STORE_TERMINATED must be emitted exactly once")
        assertEquals("onProcessorCompletion", terminatedLogs[0].sourceFunction)

        // Verify payload values (state/intent) are NOT dumped into log messages
        for (log in logger.events) {
            assertFalse(log.message.contains("sensitive_state_secret"))
            assertFalse(log.message.contains("FailingIntent"))
        }

        // Subsequent dispatch must fail with original IllegalStateException cause preserved
        val dispatchException = assertFailsWith<IllegalStateException> {
            store.dispatch(TestIntent.Increment(5))
        }
        assertEquals("Database corrupted", dispatchException.message)

        // Prove state was not changed by any later intent (remains at initial state)
        assertEquals(initialState, store.state.value, "Store state must remain unchanged after termination")
    }

    @Test
    fun verifyBlankOrWhitespaceStoreIdIsRejected() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val logger = TestLogger()

        assertFailsWith<IllegalArgumentException> {
            createStore<TestState, TestIntent, TestEffect>(
                scope = testScope,
                initialState = TestState(),
                storeId = "",
                logger = logger,
                processor = {}
            )
        }

        assertFailsWith<IllegalArgumentException> {
            createStore<TestState, TestIntent, TestEffect>(
                scope = testScope,
                initialState = TestState(),
                storeId = "   ",
                logger = logger,
                processor = {}
            )
        }
    }
}
