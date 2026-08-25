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
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
            traceSink = TraceSink(traces::add),
        )
        val taskId = BackgroundTaskId("sync")
        val registry = BackgroundTaskHandlerRegistry(
            listOf(
                object : BackgroundTaskHandler {
                    override val id: BackgroundTaskId = taskId
                    override suspend fun execute(input: Map<String, String>): BackgroundExecutionResult =
                        BackgroundExecutionResult.Success
                },
            ),
        )
        val runner = BackgroundTaskRunner(
            registry = registry,
            observability = observability,
            clock = SequenceClock(listOf(10L, 35L)),
            correlationIdProvider = CorrelationIdProvider { CorrelationId("background-42") },
        )

        assertEquals(BackgroundExecutionResult.Success, runner.run(taskId, emptyMap()))
        assertEquals(1, metrics.size)
        assertEquals("background.task", metrics.single().name)
        assertEquals(25L, metrics.single().durationMillis)
        assertEquals("background-42", metrics.single().correlationId)
        assertEquals("sync", metrics.single().attributes.getValue("task_id").value)
        assertEquals("success", metrics.single().attributes.getValue("outcome").value)
        assertEquals(1, traces.size)
        assertEquals("background-42", traces.single().correlationId.value)
        assertEquals(TraceOutcome.SUCCESS, traces.single().outcome)
    }

    private class SequenceClock(values: List<Long>) : Clock {
        private val iterator = values.iterator()
        override fun nowEpochMilliseconds(): Long = iterator.next()
    }
}
