package com.carbroz.partner.domain.capabilities.location

/**
 * Immutable value object representing a geographic coordinate pair.
 */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude.isFinite()) { "Latitude must be a finite number" }
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude.isFinite()) { "Longitude must be a finite number" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
    }
}
