package com.carbroz.partner.infrastructure.capabilities.media

import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.media.MediaPickerGateway
import com.carbroz.partner.domain.capabilities.media.MediaType
import com.carbroz.partner.domain.capabilities.media.SelectedMedia

/**
 * iOS native Apple implementation of [MediaPickerGateway].
 */
public class IosMediaPickerCapability : MediaPickerGateway {

    override suspend fun pickMedia(mediaType: MediaType): CapabilityResult<SelectedMedia> {
        return CapabilityResult.Unsupported(
            reason = "Media picker is not configured on iOS simulator environment"
        )
    }
}
