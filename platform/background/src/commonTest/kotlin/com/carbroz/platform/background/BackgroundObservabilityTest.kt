package com.carbroz.platform.background

import com.carbroz.foundation.observability.CorrelationId
import com.carbroz.foundation.observability.CorrelationIdProvider
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ObservabilityPolicy
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.PerformanceSink
import com.carbroz.foundation.observability.TraceOutcome
import com.carbroz.foundation.observability.TraceSink
import com.carbroz.foundation.observability.TraceSpan
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackgroundObservabilityTest {
    @Test
    fun `task execution records correlated duration outcome and trace`() = runTest {
        val metrics = mutableListOf<PerformanceMetric>()
        val traces = mutableListOf<TraceSpan>()
        val correlationId = CorrelationId("background-42")
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
            traceSink = TraceSink(traces::add),
        )
        val taskId = BackgroundTaskId("sync")
        val registry = BackgroundTaskHandlerRegistry(
            listOf(
                handler(taskId, BackgroundExecutionResult.Success),
            ),
        )
        val runner = BackgroundTaskRunner(
            registry = registry,
            observability = observability,
            clock = SequenceClock(listOf(10L, 35L)),
            correlationIdProvider = CorrelationIdProvider { correlationId },
        )

        assertEquals(BackgroundExecutionResult.Success, runner.run(taskId, emptyMap()))
        assertEquals(1, metrics.size)
        assertEquals("background.task", metrics.single().name)
        assertEquals(25L, metrics.single().durationMillis)
        assertEquals(correlationId, metrics.single().correlationId)
        assertEquals("sync", metrics.single().attributes.getValue("task_id").value)
        assertEquals("success", metrics.single().attributes.getValue("outcome").value)
        assertEquals(1, traces.size)
        assertEquals(correlationId, traces.single().correlationId)
        assertEquals(TraceOutcome.SUCCESS, traces.single().outcome)
    }

    @Test
    fun `retry execution is traced as retry rather than terminal failure`() = runTest {
        val traces = mutableListOf<TraceSpan>()
        val taskId = BackgroundTaskId("retrying-sync")
        val runner = BackgroundTaskRunner(
            registry = BackgroundTaskHandlerRegistry(listOf(handler(taskId, BackgroundExecutionResult.Retry))),
            observability = Observability(
                policy = ObservabilityPolicy(),
                traceSink = TraceSink(traces::add),
            ),
            clock = SequenceClock(listOf(1L, 2L)),
        )

        assertEquals(BackgroundExecutionResult.Retry, runner.run(taskId, emptyMap()))
        assertEquals(TraceOutcome.RETRY, traces.single().outcome)
    }

    private fun handler(id: BackgroundTaskId, result: BackgroundExecutionResult): BackgroundTaskHandler =
        object : BackgroundTaskHandler {
            override val id: BackgroundTaskId = id
            override suspend fun execute(input: Map<String, String>): BackgroundExecutionResult = result
        }

    private class SequenceClock(values: List<Long>) : Clock {
        private val iterator = values.iterator()
        override fun nowEpochMilliseconds(): Long = iterator.next()
    }
}
