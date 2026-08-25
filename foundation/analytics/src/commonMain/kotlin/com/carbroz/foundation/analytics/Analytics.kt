package com.carbroz.foundation.analytics

/** Analytics attribute classification. Sensitive values are never emitted verbatim. */
enum class AnalyticsSensitivity { PUBLIC, SENSITIVE }

data class AnalyticsAttribute(
    val value: String,
    val sensitivity: AnalyticsSensitivity = AnalyticsSensitivity.PUBLIC,
)

data class AnalyticsEvent(
    val name: String,
    val attributes: Map<String, AnalyticsAttribute> = emptyMap(),
)

fun interface AnalyticsSink { fun track(event: AnalyticsEvent) }

data class AnalyticsPolicy(
    val enabled: Boolean,
    val allowedEventNames: Set<String>,
    val maxAttributes: Int = 32,
) {
    init {
        require(maxAttributes in 0..128)
        require(allowedEventNames.none(String::isBlank))
    }
}

/** Canonical analytics boundary, intentionally separate from operational diagnostics. */
class AnalyticsTracker(
    private val policy: AnalyticsPolicy,
    private val sink: AnalyticsSink = AnalyticsSink {},
) {
    fun track(event: AnalyticsEvent) {
        if (!policy.enabled || event.name !in policy.allowedEventNames) return
        sink.track(event.copy(attributes = event.attributes.sanitized(policy.maxAttributes)))
    }
}

private fun Map<String, AnalyticsAttribute>.sanitized(maxAttributes: Int): Map<String, AnalyticsAttribute> =
    entries.take(maxAttributes).associate { (key, attribute) ->
        key to if (attribute.sensitivity == AnalyticsSensitivity.SENSITIVE) {
            AnalyticsAttribute("[REDACTED]")
        } else {
            attribute
        }
    }
