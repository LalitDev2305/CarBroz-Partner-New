package com.carbroz.partner.domain.capabilities.location

/**
 * Immutable snapshot of a acquired location reading.
 */
data class LocationSnapshot(
    val coordinate: GeoCoordinate,
    val accuracyMeters: Float,
    val timestampEpochMs: Long
) {
    init {
        require(accuracyMeters.isFinite()) { "Accuracy must be a finite number" }
        require(accuracyMeters >= 0.0f) { "Accuracy must not be negative" }
        require(timestampEpochMs > 0L) { "Timestamp epoch milliseconds must be positive" }
    }
}
