package com.carbroz.data.network

import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ObservabilityPolicy
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.PerformanceSink
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkPerformanceTest {
    @Test
    fun `network execution records correlated duration and sanitized outcome`() = runTest {
        val metrics = mutableListOf<PerformanceMetric>()
        val observability = Observability(
            policy = ObservabilityPolicy(),
            performanceSink = PerformanceSink(metrics::add),
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
    }

    private class SequenceClock(values: List<Long>) : Clock {
        private val iterator = values.iterator()
        override fun nowEpochMilliseconds(): Long = iterator.next()
    }
}
