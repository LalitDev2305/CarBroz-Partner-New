package com.carbroz.foundation.capabilities

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@kotlin.jvm.JvmInline
value class CapabilityId(val value: String) {
    init { require(value.isNotBlank()) { "CapabilityId must not be blank" } }
}

enum class CapabilityKind {
    PERMISSION,
    LOCATION,
    TRACKING,
    MAPS,
    CAMERA,
    MEDIA,
    NOTIFICATIONS,
    SHARING,
    EXTERNAL_URI,
}

sealed interface CapabilityAvailability {
    data object Available : CapabilityAvailability
    data class Restricted(val reason: String) : CapabilityAvailability
    data class Unavailable(val reason: String) : CapabilityAvailability
    data class Unsupported(val reason: String) : CapabilityAvailability
}

sealed interface CapabilityResult {
    data class Success(val payload: JsonObject = JsonObject(emptyMap())) : CapabilityResult
    data object Cancelled : CapabilityResult
    data class Restricted(val reason: String) : CapabilityResult
    data class Unavailable(val reason: String) : CapabilityResult
    data class Unsupported(val reason: String) : CapabilityResult
    data class Failure(val code: String, val message: String) : CapabilityResult {
        init {
            require(code.isNotBlank()) { "Capability failure code must not be blank" }
            require(message.isNotBlank()) { "Capability failure message must not be blank" }
        }
    }
}

interface CapabilityRequest {
    val kind: CapabilityKind
    val operation: String
    val arguments: Map<String, JsonElement>
}

data class GenericCapabilityRequest(
    override val kind: CapabilityKind,
    override val operation: String,
    override val arguments: Map<String, JsonElement> = emptyMap(),
) : CapabilityRequest {
    init { require(operation.isNotBlank()) { "Capability operation must not be blank" } }
}

enum class PermissionKind {
    LOCATION_WHEN_IN_USE,
    LOCATION_ALWAYS,
    CAMERA,
    MEDIA_LIBRARY,
    NOTIFICATIONS,
}

enum class PermissionState {
    GRANTED,
    DENIED,
    RESTRICTED,
    NOT_DETERMINED,
    UNSUPPORTED,
}

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitude must be finite and within -90..90" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitude must be finite and within -180..180" }
    }
}

data class LocationSnapshot(
    val coordinate: GeoCoordinate,
    val accuracyMeters: Double,
    val capturedAtEpochMilliseconds: Long,
) {
    init {
        require(accuracyMeters.isFinite() && accuracyMeters >= 0.0) { "Location accuracy must be finite and non-negative" }
        require(capturedAtEpochMilliseconds >= 0L) { "Location timestamp must be non-negative" }
    }
}
