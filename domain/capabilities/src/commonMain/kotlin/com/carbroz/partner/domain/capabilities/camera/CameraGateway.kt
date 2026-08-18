package com.carbroz.partner.domain.capabilities.camera

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain gateway for hardware camera photo capture.
 */
interface CameraGateway {
    suspend fun captureImage(): CapabilityResult<CapturedImage>
}
