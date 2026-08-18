package com.carbroz.partner.domain.capabilities.media

/**
 * Immutable domain model representing media selected from device gallery.
 */
data class SelectedMedia(
    val uri: String,
    val mediaType: MediaType,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    init {
        require(uri.isNotBlank()) { "SelectedMedia URI must not be blank" }
        require(fileName.isNotBlank()) { "SelectedMedia fileName must not be blank" }
        require(mimeType.isNotBlank()) { "SelectedMedia mimeType must not be blank" }
        require(sizeBytes >= 0L) { "SelectedMedia sizeBytes must not be negative" }
    }
}
