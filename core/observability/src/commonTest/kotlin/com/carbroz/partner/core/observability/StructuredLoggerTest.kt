package com.carbroz.partner.core.observability

import com.carbroz.partner.core.observability.logger.DefaultPipelineLogger
import com.carbroz.partner.core.observability.model.AttributeSensitivity
import com.carbroz.partner.core.observability.model.Clock
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.LogValue
import com.carbroz.partner.core.observability.model.TraceContext
import com.carbroz.partner.core.observability.policy.ObservabilityConfig
import com.carbroz.partner.core.observability.sink.LogSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StructuredLoggerTest {

    private class TestSink : LogSink {
        val events = mutableListOf<LogEvent>()
        override fun send(event: LogEvent) {
            events.add(event)
        }
    }

    private class ThrowingSink : LogSink {
        override fun send(event: LogEvent) {
            throw RuntimeException("Sink failure simulation")
        }
    }

    @Test
    fun verifyLevelFiltering() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = Clock { 1000L },
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        bound.debug("testFunc", LogCategory.APP, "debug_event", "Debug msg")
        bound.info("testFunc", LogCategory.APP, "info_event", "Info msg")

        assertEquals(1, sink.events.size)
        assertEquals(LogLevel.INFO, sink.events[0].level)
        assertEquals("info_event", sink.events[0].event)
    }

    @Test
    fun verifySensitivityPolicyFiltering() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(
                minLevel = LogLevel.DEBUG,
                allowDetailedDiagnostics = false,
                allowPayloadLogging = false
            ),
            clock = Clock { 1000L },
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        val attributes = mapOf(
            "public_key" to LogAttribute(LogValue.Text("public_val"), AttributeSensitivity.PUBLIC),
            "detail_key" to LogAttribute(LogValue.Text("detail_val"), AttributeSensitivity.DETAIL),
            "payload_key" to LogAttribute(LogValue.Text("payload_val"), AttributeSensitivity.PAYLOAD)
        )

        bound.info("testFunc", LogCategory.HTTP, "req_event", "HTTP req", attributes = attributes)

        assertEquals(1, sink.events.size)
        val eventAttrs = sink.events[0].attributes
        assertTrue(eventAttrs.containsKey("public_key"))
        assertEquals(false, eventAttrs.containsKey("detail_key"))
        assertEquals(false, eventAttrs.containsKey("payload_key"))
    }

    @Test
    fun verifyBoundLoggerIdentityAndContextEnrichment() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = Clock { 5000L },
            sinks = listOf(sink)
        )
        val bound = logger.withSource("SplashStore")
        val context = TraceContext(traceId = "trace_123", screenId = "splash")

        bound.info("initialize", LogCategory.LIFECYCLE, "bootstrap_start", "Starting app", traceContext = context)

        assertEquals(1, sink.events.size)
        val event = sink.events[0]
        assertEquals(5000L, event.timestampMs)
        assertEquals("SplashStore", event.sourceClass)
        assertEquals("initialize", event.sourceFunction)
        assertEquals("bootstrap_start", event.event)
        assertNotNull(event.traceContext)
        assertEquals("trace_123", event.traceContext.traceId)
        assertEquals("splash", event.traceContext.screenId)
    }


    @Test
    fun verifyMultiSinkDispatchAndErrorIsolation() {
        val healthySink1 = TestSink()
        val throwingSink = ThrowingSink()
        val healthySink2 = TestSink()

        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = Clock { 2000L },
            sinks = listOf(healthySink1, throwingSink, healthySink2)
        )
        val bound = logger.withSource("TestSource")

        // Execution must not throw exception despite throwingSink
        bound.info("testFunc", LogCategory.APP, "event_test", "Msg test")

        assertEquals(1, healthySink1.events.size)
        assertEquals(1, healthySink2.events.size)
    }

    @Test
    fun verifyCategoryFilteringAndEmptyCategories() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(
                minLevel = LogLevel.INFO,
                enabledCategories = setOf(LogCategory.UI, LogCategory.MVI)
            ),
            clock = Clock { 1000L },
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        bound.info("testFunc", LogCategory.HTTP, "http_event", "HTTP msg")
        bound.info("testFunc", LogCategory.UI, "ui_event", "UI msg")

        assertEquals(1, sink.events.size)
        assertEquals(LogCategory.UI, sink.events[0].category)

        // Empty category configuration drops all events
        val emptyLogger = DefaultPipelineLogger(
            config = ObservabilityConfig(enabledCategories = emptySet()),
            sinks = listOf(sink)
        )
        emptyLogger.withSource("TestSource").info("testFunc", LogCategory.UI, "ui_event", "UI msg")
        assertEquals(1, sink.events.size)
    }

    @Test
    fun verifyPayloadLoggingEnabledDoesNotBypassSecurityRedaction() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(
                minLevel = LogLevel.DEBUG,
                allowPayloadLogging = true
            ),
            clock = Clock { 1000L },
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        val attributes = mapOf(
            "payload_data" to LogAttribute(
                LogValue.Structure(mapOf("access_token" to LogAttribute(LogValue.Text("raw_jwt_secret")))),
                AttributeSensitivity.PAYLOAD
            )
        )

        bound.info("testFunc", LogCategory.HTTP, "payload_event", "Payload msg", attributes = attributes)

        assertEquals(1, sink.events.size)
        val payloadVal = sink.events[0].attributes["payload_data"] as LogValue.Structure
        assertEquals(
            LogValue.Text("[REDACTED_SECRET]"),
            (payloadVal.attributes["access_token"] as LogAttribute).value
        )
    }

    @Test
    fun verifyTraceContextMergePrecedence() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = Clock { 1000L },
            sinks = listOf(sink)
        )
        val baseContext = TraceContext(traceId = "trace-A", screenId = "screen-A", operationId = "op-A")
        val operationContext = TraceContext(traceId = "trace-A", screenId = "screen-B", requestId = "req-B")

        val bound = logger.withSource("TestSource", defaultTraceContext = baseContext)
        bound.info("testFunc", LogCategory.APP, "merge_event", "Merge msg", traceContext = operationContext)

        val eventContext = sink.events[0].traceContext
        assertNotNull(eventContext)
        assertEquals("trace-A", eventContext.traceId)
        assertEquals("screen-B", eventContext.screenId) // Operation context overrides bound screenId
        assertEquals("op-A", eventContext.operationId)  // Bound operationId preserved
        assertEquals("req-B", eventContext.requestId)   // Operation requestId merged
    }

    @Test
    fun verifyMultiSinkFailureIsolationAcrossMultipleFailures() {
        val sinkA = ThrowingSink()
        val sinkB = TestSink()
        val sinkC = ThrowingSink()
        val sinkD = TestSink()

        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = Clock { 2000L },
            sinks = listOf(sinkA, sinkB, sinkC, sinkD)
        )
        val bound = logger.withSource("TestSource")

        bound.info("testFunc", LogCategory.APP, "multi_failure_event", "Msg test")

        assertEquals(1, sinkB.events.size)
        assertEquals(1, sinkD.events.size)
    }

    @Test
    fun verifyCallerMapImmutability() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = Clock { 1000L },
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        val rawMap = mutableMapOf(
            "authorization" to LogAttribute(LogValue.Text("Bearer secret"))
        )

        bound.info("testFunc", LogCategory.SECURITY, "auth_event", "Auth msg", attributes = rawMap)

        // Original map must remain unchanged
        assertEquals(LogValue.Text("Bearer secret"), rawMap["authorization"]?.value)
        // Sink event must be redacted
        assertEquals(LogValue.Text("[REDACTED_SECRET]"), sink.events[0].attributes["authorization"])
    }
}

