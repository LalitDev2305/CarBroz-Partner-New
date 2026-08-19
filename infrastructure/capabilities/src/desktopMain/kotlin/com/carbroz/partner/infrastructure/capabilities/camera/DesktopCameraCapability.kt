package com.carbroz.partner.infrastructure.capabilities.camera

import com.carbroz.partner.domain.capabilities.camera.CameraGateway
import com.carbroz.partner.domain.capabilities.camera.CapturedImage
import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Desktop JVM implementation of [CameraGateway].
 *
 * Explicitly returns [CapabilityResult.Unsupported] rather than crashing or faking photo captures.
 */
public class DesktopCameraCapability : CameraGateway {

    override suspend fun captureImage(): CapabilityResult<CapturedImage> {
        return CapabilityResult.Unsupported(
            reason = "Camera capture is not supported on Desktop platform"
        )
    }
}
