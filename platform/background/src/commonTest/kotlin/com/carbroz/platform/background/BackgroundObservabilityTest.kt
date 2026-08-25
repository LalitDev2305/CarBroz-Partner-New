package com.carbroz.platform.background

import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ObservabilityPolicy
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.PerformanceSink
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackgroundObservabilityTest {
    @Test
    fun `task execution records duration and outcome`() = runTest {
        val metrics = mutableListOf<PerformanceMetric>()
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
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
        )

        assertEquals(BackgroundExecutionResult.Success, runner.run(taskId, emptyMap()))
        assertEquals(1, metrics.size)
        assertEquals("background.task", metrics.single().name)
        assertEquals(25L, metrics.single().durationMillis)
        assertEquals("sync", metrics.single().attributes.getValue("task_id").value)
        assertEquals("success", metrics.single().attributes.getValue("outcome").value)
    }

    private class SequenceClock(values: List<Long>) : Clock {
        private val iterator = values.iterator()
        override fun nowEpochMilliseconds(): Long = iterator.next()
    }
}
