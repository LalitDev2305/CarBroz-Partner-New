package com.carbroz.partner.domain.capabilities.camera

/**
 * Immutable domain model representing a photo captured by device camera.
 */
data class CapturedImage(
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    init {
        require(uri.isNotBlank()) { "CapturedImage URI must not be blank" }
        require(mimeType.isNotBlank()) { "CapturedImage MIME type must not be blank" }
        require(sizeBytes >= 0L) { "CapturedImage sizeBytes must not be negative" }
    }
}
