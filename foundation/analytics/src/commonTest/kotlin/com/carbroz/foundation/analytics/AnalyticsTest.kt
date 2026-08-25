package com.carbroz.foundation.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsTest {
    @Test
    fun onlyAllowlistedEventsReachSink() {
        val received = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker(
            policy = AnalyticsPolicy(enabled = true, allowedEventNames = setOf("app_open")),
            sink = AnalyticsSink(received::add),
        )

        tracker.track(AnalyticsEvent("unknown"))
        tracker.track(AnalyticsEvent("app_open"))

        assertEquals(listOf("app_open"), received.map(AnalyticsEvent::name))
    }

    @Test
    fun sensitiveAttributesAreRedacted() {
        var received: AnalyticsEvent? = null
        val tracker = AnalyticsTracker(
            policy = AnalyticsPolicy(enabled = true, allowedEventNames = setOf("screen")),
            sink = AnalyticsSink { received = it },
        )

        tracker.track(
            AnalyticsEvent(
                name = "screen",
                attributes = mapOf("account" to AnalyticsAttribute("123", AnalyticsSensitivity.SENSITIVE)),
            ),
        )

        assertEquals("[REDACTED]", received?.attributes?.get("account")?.value)
    }

    @Test
    fun disabledAnalyticsDropsEvents() {
        val received = mutableListOf<AnalyticsEvent>()
        AnalyticsTracker(
            policy = AnalyticsPolicy(enabled = false, allowedEventNames = setOf("app_open")),
            sink = AnalyticsSink(received::add),
        ).track(AnalyticsEvent("app_open"))

        assertTrue(received.isEmpty())
    }

    @Test
    fun throwingAnalyticsSinkNeverEscapesIntoProductControlFlow() {
        val tracker = AnalyticsTracker(
            policy = AnalyticsPolicy(enabled = true, allowedEventNames = setOf("app_open")),
            sink = AnalyticsSink { error("vendor unavailable") },
        )

        tracker.track(AnalyticsEvent("app_open"))
    }

    @Test
    fun publicAnalyticsAttributesAreBoundedBeforeSink() {
        var received: AnalyticsEvent? = null
        val tracker = AnalyticsTracker(
            policy = AnalyticsPolicy(enabled = true, allowedEventNames = setOf("screen")),
            sink = AnalyticsSink { received = it },
        )

        tracker.track(
            AnalyticsEvent(
                name = "screen",
                attributes = mapOf("k".repeat(200) to AnalyticsAttribute("v".repeat(1_000))),
            ),
        )

        val event = requireNotNull(received)
        assertTrue(event.attributes.keys.single().length <= 80)
        assertTrue(event.attributes.values.single().value.length <= 512)
    }
}
