package com.carbroz.partner.infrastructure.capabilities.external

import com.carbroz.partner.domain.capabilities.core.CapabilityFailure
import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.external.ExternalUriLauncher
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS native Apple implementation of [ExternalUriLauncher].
 *
 * Opens web links in the default Safari browser via UIApplication.sharedApplication.openURL.
 */
public class IosExternalUriCapability : ExternalUriLauncher {

    override suspend fun openUri(uri: String): CapabilityResult<Unit> {
        val nsUrl = NSURL.URLWithString(uri)
            ?: return CapabilityResult.Failure(
                CapabilityFailure(
                    code = CapabilityFailure.FailureCode.UNKNOWN,
                    message = "Invalid URL string format: '$uri'"
                )
            )

        return try {
            val app = UIApplication.sharedApplication
            if (app.canOpenURL(nsUrl)) {
                app.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
                CapabilityResult.Success(Unit)
            } else {
                CapabilityResult.Unsupported(
                    reason = "iOS application cannot open URI scheme: '$uri'"
                )
            }
        } catch (e: Exception) {
            CapabilityResult.Failure(
                CapabilityFailure(
                    code = CapabilityFailure.FailureCode.UNKNOWN,
                    message = e.message ?: "Failed to open URL on iOS: '$uri'"
                )
            )
        }
    }
}
