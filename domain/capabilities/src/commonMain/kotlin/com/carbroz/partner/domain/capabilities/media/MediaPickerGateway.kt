package com.carbroz.partner.domain.capabilities.media

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain gateway for selecting photos/videos from device media gallery.
 */
interface MediaPickerGateway {
    suspend fun pickMedia(
        mediaType: MediaType = MediaType.IMAGE
    ): CapabilityResult<SelectedMedia>
}
