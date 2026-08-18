package com.carbroz.partner.domain.capabilities.document

/**
 * Immutable domain model representing a document file selected via file chooser.
 */
data class SelectedDocument(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    init {
        require(uri.isNotBlank()) { "SelectedDocument URI must not be blank" }
        require(fileName.isNotBlank()) { "SelectedDocument fileName must not be blank" }
        require(mimeType.isNotBlank()) { "SelectedDocument mimeType must not be blank" }
        require(sizeBytes >= 0L) { "SelectedDocument sizeBytes must not be negative" }
    }
}
