package com.carbroz.partner.domain.capabilities.document

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain gateway for selecting documents/files via system file chooser.
 */
interface DocumentPickerGateway {
    suspend fun pickDocument(
        allowedMimeTypes: List<String> = emptyList()
    ): CapabilityResult<SelectedDocument>
}
