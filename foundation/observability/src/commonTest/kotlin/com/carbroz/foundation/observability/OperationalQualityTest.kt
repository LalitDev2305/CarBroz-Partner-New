package com.carbroz.foundation.observability

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OperationalQualityTest {
    @Test
    fun correlationIdsAreOpaqueBoundedAndUniqueAcrossCalls() {
        val provider = RandomCorrelationIdProvider(Random(7))

        val first = provider.next("Network Request")
        val second = provider.next("Network Request")

        assertTrue(first.value.startsWith("networkrequest-"))
        assertTrue(first.value.length <= CorrelationId.MAX_LENGTH)
        assertNotEquals(first, second)
    }

    @Test
    fun traceAttributesAreRedactedBeforeSink() {
        var received: TraceSpan? = null
        val observability = Observability(
            policy = ObservabilityPolicy(),
            traceSink = TraceSink { received = it },
        )
        observability.trace(
            TraceSpan(
                name = "network.request",
                correlationId = CorrelationId("network-1"),
                durationMillis = 5,
                outcome = TraceOutcome.SUCCESS,
                attributes = mapOf(
                    "method" to DiagnosticAttribute("GET"),
                    "token" to DiagnosticAttribute("secret", DiagnosticSensitivity.SENSITIVE),
                ),
            ),
        )

        assertEquals("GET", received?.attributes?.get("method")?.value)
        assertEquals("[REDACTED]", received?.attributes?.get("token")?.value)
    }

    @Test
    fun throwableDetailsAreSuppressedByDefault() {
        var receivedThrowable: Throwable? = IllegalStateException("sentinel")
        val observability = Observability(
            policy = ObservabilityPolicy(),
            crashSink = CrashSink { _, throwable -> receivedThrowable = throwable },
        )

        observability.crash(
            CrashEvent("startup", "unexpected_failure"),
            IllegalStateException("credential-like detail"),
        )

        assertNull(receivedThrowable)
    }

    @Test
    fun resourceReporterEmitsAvailableSnapshot() {
        var received: ResourceSnapshot? = null
        val observability = Observability(
            policy = ObservabilityPolicy(),
            resourceSink = ResourceSink { received = it },
        )
        val expected = ResourceSnapshot(heapUsedBytes = 20, heapLimitBytes = 100, processorCount = 4)
        val reporter = ResourceDiagnosticsReporter(
            diagnostics = ResourceDiagnostics { ResourceDiagnosticResult.Available(expected) },
            observability = observability,
        )

        val result = reporter.sample()

        assertIs<ResourceDiagnosticResult.Available>(result)
        assertEquals(expected, received)
    }

    @Test
    fun disabledTraceAndResponsivenessPoliciesEmitNothing() {
        var traceCalled = false
        var responsivenessCalled = false
        val observability = Observability(
            policy = ObservabilityPolicy(
                tracingEnabled = false,
                responsivenessReportingEnabled = false,
            ),
            traceSink = TraceSink { traceCalled = true },
            responsivenessSink = ResponsivenessSink { responsivenessCalled = true },
        )

        observability.trace(
            TraceSpan("operation", CorrelationId("op-1"), durationMillis = 1, outcome = TraceOutcome.SUCCESS),
        )
        observability.responsiveness(
            ResponsivenessIncident("main_thread", blockedMillis = 5_000, thresholdMillis = 5_000),
        )

        assertFalse(traceCalled)
        assertFalse(responsivenessCalled)
    }

    @Test
    fun closingMonitorDoesNotCancelExternallyOwnedScopeAndPreventsRestart() {
        val externalJob = SupervisorJob()
        val monitor = MainThreadResponsivenessMonitor(
            dispatcher = MainThreadDispatcher { it() },
            observability = NoOpObservability,
            externalScope = CoroutineScope(externalJob),
        )

        monitor.close()

        assertTrue(externalJob.isActive)
        assertEquals(ResponsivenessMonitorStartResult.Closed, monitor.start())
        externalJob.cancel()
    }
}
