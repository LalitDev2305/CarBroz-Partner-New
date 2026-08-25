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
}
