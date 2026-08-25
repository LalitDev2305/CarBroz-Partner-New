package com.carbroz.foundation.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObservabilityTest {
    @Test
    fun sensitiveAttributesAreRedactedBeforeSink() {
        var received: LogEvent? = null
        val observability = Observability(
            policy = ObservabilityPolicy(minimumLogLevel = LogLevel.DEBUG),
            logSink = LogSink { received = it },
        )

        observability.log(
            LogEvent(
                level = LogLevel.INFO,
                category = "network",
                message = "request",
                attributes = mapOf(
                    "endpoint" to DiagnosticAttribute("/bootstrap"),
                    "token" to DiagnosticAttribute("secret", DiagnosticSensitivity.SENSITIVE),
                ),
            ),
        )

        assertEquals("/bootstrap", received?.attributes?.get("endpoint")?.value)
        assertEquals("[REDACTED]", received?.attributes?.get("token")?.value)
    }

    @Test
    fun policySuppressesDisabledDiagnostics() {
        val logs = mutableListOf<LogEvent>()
        val crashes = mutableListOf<CrashEvent>()
        val metrics = mutableListOf<PerformanceMetric>()
        val observability = Observability(
            policy = ObservabilityPolicy(
                minimumLogLevel = LogLevel.WARN,
                crashReportingEnabled = false,
                performanceMetricsEnabled = false,
            ),
            logSink = LogSink(logs::add),
            crashSink = CrashSink { event, _ -> crashes += event },
            performanceSink = PerformanceSink(metrics::add),
        )

        observability.log(LogEvent(LogLevel.INFO, "startup", "ignored"))
        observability.crash(CrashEvent("startup", "ignored"))
        observability.performance(PerformanceMetric("startup", 1))

        assertTrue(logs.isEmpty())
        assertTrue(crashes.isEmpty())
        assertTrue(metrics.isEmpty())
    }

    @Test
    fun throwingSinksNeverEscapeIntoApplicationControlFlow() {
        val observability = Observability(
            policy = ObservabilityPolicy(minimumLogLevel = LogLevel.DEBUG),
            logSink = LogSink { error("log sink failed") },
            crashSink = CrashSink { _, _ -> error("crash sink failed") },
            performanceSink = PerformanceSink { error("performance sink failed") },
            traceSink = TraceSink { error("trace sink failed") },
            responsivenessSink = ResponsivenessSink { error("responsiveness sink failed") },
            resourceSink = ResourceSink { error("resource sink failed") },
        )

        observability.log(LogEvent(LogLevel.INFO, "test", "log"))
        observability.crash(CrashEvent("test", "crash"), IllegalStateException("private detail"))
        observability.performance(PerformanceMetric("test.metric", 1))
        observability.trace(
            TraceSpan("test.trace", CorrelationId("test-1"), durationMillis = 1, outcome = TraceOutcome.SUCCESS),
        )
        observability.responsiveness(ResponsivenessIncident("main_thread", 5_000, 5_000))
        observability.resource(ResourceSnapshot(heapUsedBytes = 1))
    }

    @Test
    fun publicDiagnosticValuesAreBoundedBeforeSink() {
        var received: LogEvent? = null
        val observability = Observability(
            policy = ObservabilityPolicy(minimumLogLevel = LogLevel.DEBUG),
            logSink = LogSink { received = it },
        )

        observability.log(
            LogEvent(
                level = LogLevel.INFO,
                category = "c".repeat(200),
                message = "m".repeat(400),
                attributes = mapOf("k".repeat(200) to DiagnosticAttribute("v".repeat(1_000))),
            ),
        )

        val event = requireNotNull(received)
        assertTrue(event.category.length <= 80)
        assertTrue(event.message.length <= 160)
        assertTrue(event.attributes.keys.single().length <= 80)
        assertTrue(event.attributes.values.single().value.length <= 512)
    }
}
