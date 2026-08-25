package com.carbroz.runtime.application.startup

import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ObservabilityPolicy
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.PerformanceSink
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupObservabilityTest {
    @Test
    fun `successful startup records task and total duration`() = runTest {
        val metrics = mutableListOf<PerformanceMetric>()
        val clock = SequenceClock(listOf(100L, 110L, 130L, 160L))
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
        )
        val coordinator = StartupCoordinator(
            tasks = listOf(successTask("session")),
            observability = observability,
            clock = clock,
        )

        assertEquals(StartupResult.Ready, coordinator.run())
        assertEquals(listOf("startup.task", "startup.total"), metrics.map { it.name })
        assertEquals(20L, metrics[0].durationMillis)
        assertEquals(60L, metrics[1].durationMillis)
        assertEquals("session", metrics[0].attributes.getValue("task_id").value)
        assertEquals("success", metrics[0].attributes.getValue("outcome").value)
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
