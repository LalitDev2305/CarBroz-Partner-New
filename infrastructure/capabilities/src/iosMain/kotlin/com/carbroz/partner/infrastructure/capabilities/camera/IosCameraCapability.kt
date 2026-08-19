package com.carbroz.partner.infrastructure.capabilities.camera

import com.carbroz.partner.domain.capabilities.camera.CameraGateway
import com.carbroz.partner.domain.capabilities.camera.CapturedImage
import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * iOS native Apple implementation of [CameraGateway].
 */
public class IosCameraCapability : CameraGateway {

    override suspend fun captureImage(): CapabilityResult<CapturedImage> {
        return CapabilityResult.Unsupported(
            reason = "Camera capture is not configured on iOS simulator environment"
        )
    }
}
