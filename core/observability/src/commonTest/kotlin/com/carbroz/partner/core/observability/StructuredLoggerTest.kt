package com.carbroz.partner.core.observability

import com.carbroz.partner.core.observability.logger.DefaultPipelineLogger
import com.carbroz.partner.core.observability.model.AttributeSensitivity
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

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

    private fun testClock(epochMs: Long = 1000L): Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMs)
    }

    @Test
    fun verifyObservabilityDisabledPreventsAllEvents() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(isEnabled = false),
            clock = testClock(),
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        bound.info("testFunc", LogCategory.APP, "disabled_event", "Msg when disabled")

        assertEquals(0, sink.events.size)
    }

    @Test
    fun verifyOperationalSinkRequired() {
        assertFailsWith<IllegalArgumentException> {
            DefaultPipelineLogger(sinks = emptyList())
        }
    }

    @Test
    fun verifyLevelFiltering() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = testClock(1000L),
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
            clock = testClock(1000L),
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
    fun verifyRecursiveSensitivityPolicyFilteringStructure() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(
                minLevel = LogLevel.DEBUG,
                allowDetailedDiagnostics = false,
                allowPayloadLogging = false
            ),
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        val nestedStruct = LogValue.Structure(
            mapOf(
                "publicChild" to LogAttribute(LogValue.Text("pub"), AttributeSensitivity.PUBLIC),
                "diagChild" to LogAttribute(LogValue.Text("diag"), AttributeSensitivity.DETAIL),
                "payloadChild" to LogAttribute(LogValue.Text("pay"), AttributeSensitivity.PAYLOAD)
            )
        )
        val attributes = mapOf("structKey" to LogAttribute(nestedStruct, AttributeSensitivity.PUBLIC))

        bound.info("testFunc", LogCategory.APP, "nested_event", "Nested msg", attributes = attributes)

        assertEquals(1, sink.events.size)
        val structVal = sink.events[0].attributes["structKey"] as LogValue.Structure
        assertTrue(structVal.attributes.containsKey("publicChild"))
        assertEquals(false, structVal.attributes.containsKey("diagChild"))
        assertEquals(false, structVal.attributes.containsKey("payloadChild"))
    }

    @Test
    fun verifyRecursiveSensitivityPolicyFilteringCollectionToStructure() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(
                minLevel = LogLevel.DEBUG,
                allowDetailedDiagnostics = false,
                allowPayloadLogging = false
            ),
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        val innerStruct = LogValue.Structure(
            mapOf(
                "publicChild" to LogAttribute(LogValue.Text("pub"), AttributeSensitivity.PUBLIC),
                "detailChild" to LogAttribute(LogValue.Text("detail"), AttributeSensitivity.DETAIL),
                "payloadChild" to LogAttribute(LogValue.Text("payload"), AttributeSensitivity.PAYLOAD)
            )
        )
        val collection = LogValue.Collection(listOf(innerStruct))
        val attributes = mapOf("topCollection" to LogAttribute(collection, AttributeSensitivity.PUBLIC))

        bound.info("testFunc", LogCategory.APP, "coll_struct_event", "Collection Structure msg", attributes = attributes)

        assertEquals(1, sink.events.size)
        val topColl = sink.events[0].attributes["topCollection"] as LogValue.Collection
        assertEquals(1, topColl.items.size)
        val structItem = topColl.items[0] as LogValue.Structure
        assertTrue(structItem.attributes.containsKey("publicChild"))
        assertEquals(false, structItem.attributes.containsKey("detailChild"))
        assertEquals(false, structItem.attributes.containsKey("payloadChild"))
    }

    @Test
    fun verifyPerformanceTimingConfigControl() {
        val sink = TestSink()
        val disabledLogger = DefaultPipelineLogger(
            config = ObservabilityConfig(enablePerformanceTiming = false),
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        disabledLogger.withSource("TestSource").log(
            level = LogLevel.INFO,
            category = LogCategory.APP,
            sourceFunction = "testFunc",
            event = "timing_disabled",
            message = "Timing disabled test",
            durationMs = 123L
        )

        assertEquals(1, sink.events.size)
        assertNull(sink.events[0].durationMs)

        val enabledSink = TestSink()
        val enabledLogger = DefaultPipelineLogger(
            config = ObservabilityConfig(enablePerformanceTiming = true),
            clock = testClock(1000L),
            sinks = listOf(enabledSink)
        )
        enabledLogger.withSource("TestSource").log(
            level = LogLevel.INFO,
            category = LogCategory.APP,
            sourceFunction = "testFunc",
            event = "timing_enabled",
            message = "Timing enabled test",
            durationMs = 123L
        )

        assertEquals(1, enabledSink.events.size)
        assertEquals(123L, enabledSink.events[0].durationMs)
    }

    @Test
    fun verifyBoundLoggerIdentityAndContextEnrichment() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = testClock(5000L),
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
            clock = testClock(2000L),
            sinks = listOf(healthySink1, throwingSink, healthySink2)
        )
        val bound = logger.withSource("TestSource")

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
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        bound.info("testFunc", LogCategory.HTTP, "http_event", "HTTP msg")
        bound.info("testFunc", LogCategory.UI, "ui_event", "UI msg")

        assertEquals(1, sink.events.size)
        assertEquals(LogCategory.UI, sink.events[0].category)

        val emptyLogger = DefaultPipelineLogger(
            config = ObservabilityConfig(enabledCategories = emptySet()),
            clock = testClock(1000L),
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
            clock = testClock(1000L),
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
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        val baseContext = TraceContext(traceId = "trace-A", screenId = "screen-A", operationId = "op-A")
        val operationContext = TraceContext(traceId = "trace-A", screenId = "screen-B", requestId = "req-B")

        val bound = logger.withSource("TestSource", defaultTraceContext = baseContext)
        bound.info("testFunc", LogCategory.APP, "merge_event", "Merge msg", traceContext = operationContext)

        val eventContext = sink.events[0].traceContext
        assertNotNull(eventContext)
        assertEquals("trace-A", eventContext.traceId)
        assertEquals("screen-B", eventContext.screenId)
        assertEquals("op-A", eventContext.operationId)
        assertEquals("req-B", eventContext.requestId)
    }

    @Test
    fun verifyMultiSinkFailureIsolationAcrossMultipleFailures() {
        val sinkA = ThrowingSink()
        val sinkB = TestSink()
        val sinkC = ThrowingSink()
        val sinkD = TestSink()

        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = testClock(2000L),
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
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        val bound = logger.withSource("TestSource")

        val rawMap = mutableMapOf(
            "authorization" to LogAttribute(LogValue.Text("Bearer secret"))
        )

        bound.info("testFunc", LogCategory.SECURITY, "auth_event", "Auth msg", attributes = rawMap)

        assertEquals(LogValue.Text("Bearer secret"), rawMap["authorization"]?.value)
        assertEquals(LogValue.Text("[REDACTED_SECRET]"), sink.events[0].attributes["authorization"])
    }

    @Test
    fun verifyRealClockProducesNonZeroTimestamps() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            sinks = listOf(sink)
        )
        logger.withSource("TestSource").info("testFunc", LogCategory.APP, "real_time_event", "Real time msg")

        assertEquals(1, sink.events.size)
        assertTrue(sink.events[0].timestampMs > 0L)
    }

    @Test
    fun verifyEndToEndPipelineSanitization() {
        val sink = TestSink()
        val logger = DefaultPipelineLogger(
            config = ObservabilityConfig(minLevel = LogLevel.INFO),
            clock = testClock(1000L),
            sinks = listOf(sink)
        )
        val bound = logger.withSource("AuthStore")

        val rawAttributes = mapOf(
            "userPassword" to LogAttribute(LogValue.Text("rawPass123")),
            "sessionToken" to LogAttribute(LogValue.Text("token_abc_456"))
        )

        bound.error(
            sourceFunction = "login",
            category = LogCategory.SECURITY,
            event = "auth_failed",
            message = "Login failed for raw.user@carbroz.com using password=rawPass123 Authorization: Bearer bearer_secret_token",
            throwable = RuntimeException("Connect error for raw.user@carbroz.com accessToken=secret_jwt_xyz"),
            attributes = rawAttributes
        )

        assertEquals(1, sink.events.size)
        val event = sink.events[0]

        assertTrue(event.message.contains("[REDACTED_EMAIL]"))
        assertTrue(event.message.contains("password=[REDACTED_SECRET]"))
        assertTrue(event.message.contains("Authorization: Bearer [REDACTED_SECRET]"))

        kotlin.test.assertFalse(event.message.contains("raw.user@carbroz.com"))
        kotlin.test.assertFalse(event.message.contains("rawPass123"))
        kotlin.test.assertFalse(event.message.contains("bearer_secret_token"))

        assertEquals(LogValue.Text("[REDACTED_SECRET]"), event.attributes["userPassword"])
        assertEquals(LogValue.Text("[REDACTED_SECRET]"), event.attributes["sessionToken"])

        assertNotNull(event.errorInfo)
        assertTrue(event.errorInfo!!.message!!.contains("[REDACTED_EMAIL]"))
        assertTrue(event.errorInfo!!.message!!.contains("access_token=[REDACTED_SECRET]"))
        kotlin.test.assertFalse(event.errorInfo!!.message!!.contains("secret_jwt_xyz"))
    }
}
