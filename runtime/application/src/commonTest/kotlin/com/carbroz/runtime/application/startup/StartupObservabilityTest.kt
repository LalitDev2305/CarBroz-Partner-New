package com.carbroz.runtime.application.startup

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

class StartupObservabilityTest {
    @Test
    fun `successful startup records correlated task and total duration traces`() = runTest {
        val metrics = mutableListOf<PerformanceMetric>()
        val traces = mutableListOf<TraceSpan>()
        val clock = SequenceClock(listOf(100L, 110L, 130L, 160L))
        val correlationId = CorrelationId("startup-42")
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
            traceSink = TraceSink(traces::add),
        )
        val coordinator = StartupCoordinator(
            tasks = listOf(successTask("session")),
            observability = observability,
            clock = clock,
            correlationIdProvider = CorrelationIdProvider { correlationId },
        )

        assertEquals(StartupResult.Ready, coordinator.run())
        assertEquals(listOf("startup.task", "startup.total"), metrics.map { it.name })
        assertEquals(20L, metrics[0].durationMillis)
        assertEquals(60L, metrics[1].durationMillis)
        assertEquals(correlationId, metrics[0].correlationId)
        assertEquals("session", metrics[0].attributes.getValue("task_id").value)
        assertEquals("success", metrics[0].attributes.getValue("outcome").value)
        assertEquals(listOf("startup.task", "startup.total"), traces.map { it.name })
        assertEquals(setOf(correlationId), traces.map { it.correlationId }.toSet())
        assertEquals(listOf(TraceOutcome.SUCCESS, TraceOutcome.SUCCESS), traces.map { it.outcome })
    }

    private fun successTask(id: String): StartupTask = object : StartupTask {
        override val id: String = id
        override suspend fun execute(): StartupTaskResult = StartupTaskResult.Success
    }

    private class SequenceClock(values: List<Long>) : Clock {
        private val iterator = values.iterator()
        override fun nowEpochMilliseconds(): Long = iterator.next()
    }
}
