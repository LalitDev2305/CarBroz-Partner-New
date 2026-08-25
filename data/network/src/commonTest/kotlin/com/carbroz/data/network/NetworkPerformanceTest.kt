package com.carbroz.data.network

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
import kotlin.test.assertIs

class NetworkPerformanceTest {
    @Test
    fun `network execution records correlated duration outcome and trace`() = runTest {
        val metrics = mutableListOf<PerformanceMetric>()
        val traces = mutableListOf<TraceSpan>()
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
            traceSink = TraceSink(traces::add),
        )
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { NetworkResult.Success(NetworkResponse(200)) },
            requestIdProvider = NetworkRequestIdProvider { NetworkRequestId("request-42") },
            clock = SequenceClock(listOf(100L, 145L)),
            observability = observability,
        )

        assertIs<NetworkResult.Success>(
            executor.execute(
                NetworkRequest(
                    method = NetworkMethod.GET,
                    endpoint = NetworkEndpoint("/health"),
                ),
            ),
        )
        assertEquals(1, metrics.size)
        assertEquals("network.request", metrics.single().name)
        assertEquals(45L, metrics.single().durationMillis)
        assertEquals("request-42", metrics.single().correlationId)
        assertEquals("GET", metrics.single().attributes.getValue("method").value)
        assertEquals("success", metrics.single().attributes.getValue("outcome").value)
        assertEquals(1, traces.size)
        assertEquals("request-42", traces.single().correlationId.value)
        assertEquals(TraceOutcome.SUCCESS, traces.single().outcome)
        assertEquals(45L, traces.single().durationMillis)
    }

    private class SequenceClock(values: List<Long>) : Clock {
        private val iterator = values.iterator()
        override fun nowEpochMilliseconds(): Long = iterator.next()
    }
}
