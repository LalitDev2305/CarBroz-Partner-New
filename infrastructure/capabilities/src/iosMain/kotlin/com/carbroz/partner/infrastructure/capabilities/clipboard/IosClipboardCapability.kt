package com.carbroz.partner.infrastructure.capabilities.clipboard

import com.carbroz.partner.domain.capabilities.clipboard.ClipboardGateway
import com.carbroz.partner.domain.capabilities.core.CapabilityFailure
import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import platform.UIKit.UIPasteboard

/**
 * iOS native Apple implementation of [ClipboardGateway].
 *
 * Copies plain text to system UIPasteboard.generalPasteboard.
 */
public class IosClipboardCapability : ClipboardGateway {

    override suspend fun copyText(text: String): CapabilityResult<Unit> {
        return try {
            UIPasteboard.generalPasteboard.string = text
            CapabilityResult.Success(Unit)
        } catch (e: Exception) {
            CapabilityResult.Failure(
                CapabilityFailure(
                    code = CapabilityFailure.FailureCode.UNKNOWN,
                    message = e.message ?: "Failed to copy text to iOS UIPasteboard"
                )
            )
        }
    }
}
