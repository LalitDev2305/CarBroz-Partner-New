package com.carbroz.foundation.analytics

/** Analytics attribute classification. Sensitive values are never emitted verbatim. */
enum class AnalyticsSensitivity { PUBLIC, SENSITIVE }

/** Typed analytics attribute. Values are bounded and sensitive values are redacted before sinks. */
data class AnalyticsAttribute(
    val value: String,
    val sensitivity: AnalyticsSensitivity = AnalyticsSensitivity.PUBLIC,
)

/** Product analytics event. Event names must be explicitly allow-listed by [AnalyticsPolicy]. */
data class AnalyticsEvent(
    val name: String,
    val attributes: Map<String, AnalyticsAttribute> = emptyMap(),
)

/** Vendor-neutral analytics sink. Product code must use [AnalyticsTracker], never this sink directly. */
fun interface AnalyticsSink { fun track(event: AnalyticsEvent) }

/** Explicit analytics allow-list and bounded-payload policy. */
data class AnalyticsPolicy(
    val enabled: Boolean,
    val allowedEventNames: Set<String>,
    val maxAttributes: Int = 32,
) {
    init {
        require(maxAttributes in 0..128)
        require(allowedEventNames.none(String::isBlank))
        require(allowedEventNames.all { it.length <= MAX_EVENT_NAME_LENGTH })
    }

    private companion object {
        const val MAX_EVENT_NAME_LENGTH = 120
    }
}

/**
 * Canonical analytics boundary, intentionally separate from operational diagnostics.
 *
 * Unknown events are dropped, sensitive values are redacted, payloads are bounded and sink failures are
 * isolated so analytics can never alter product control flow.
 */
class AnalyticsTracker(
    private val policy: AnalyticsPolicy,
    private val sink: AnalyticsSink = AnalyticsSink {},
) {
    fun track(event: AnalyticsEvent) {
        if (!policy.enabled || event.name !in policy.allowedEventNames) return
        runCatching {
            sink.track(
                event.copy(attributes = event.attributes.sanitized(policy.maxAttributes)),
            )
        }
    }
}

private const val REDACTED = "[REDACTED]"
private const val MAX_ATTRIBUTE_KEY_LENGTH = 80
private const val MAX_ATTRIBUTE_VALUE_LENGTH = 512

private fun Map<String, AnalyticsAttribute>.sanitized(maxAttributes: Int): Map<String, AnalyticsAttribute> =
    entries.take(maxAttributes).associate { (key, attribute) ->
        key.take(MAX_ATTRIBUTE_KEY_LENGTH) to if (attribute.sensitivity == AnalyticsSensitivity.SENSITIVE) {
            AnalyticsAttribute(REDACTED)
        } else {
            attribute.copy(value = attribute.value.take(MAX_ATTRIBUTE_VALUE_LENGTH))
        }
    }
