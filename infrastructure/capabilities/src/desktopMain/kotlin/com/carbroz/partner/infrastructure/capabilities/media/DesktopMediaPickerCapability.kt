package com.carbroz.partner.infrastructure.capabilities.media

import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.media.MediaPickerGateway
import com.carbroz.partner.domain.capabilities.media.MediaType
import com.carbroz.partner.domain.capabilities.media.SelectedMedia

/**
 * Desktop JVM implementation of [MediaPickerGateway].
 *
 * Explicitly returns [CapabilityResult.Unsupported] rather than crashing.
 */
public class DesktopMediaPickerCapability : MediaPickerGateway {

    override suspend fun pickMedia(mediaType: MediaType): CapabilityResult<SelectedMedia> {
        return CapabilityResult.Unsupported(
            reason = "Media picker is not supported on Desktop platform"
        )
    }
}
