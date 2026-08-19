package com.carbroz.partner.infrastructure.capabilities.external

import com.carbroz.partner.domain.capabilities.core.CapabilityFailure
import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.external.ExternalUriLauncher
import java.awt.Desktop
import java.net.URI

/**
 * Desktop AWT implementation of [ExternalUriLauncher].
 *
 * Opens web links in the default desktop web browser via java.awt.Desktop.
 */
public class DesktopExternalUriCapability : ExternalUriLauncher {

    override suspend fun openUri(uri: String): CapabilityResult<Unit> {
        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(uri))
                CapabilityResult.Success(Unit)
            } else {
                CapabilityResult.Unsupported(
                    reason = "Desktop browsing action is not supported on this platform environment"
                )
            }
        } catch (e: Exception) {
            CapabilityResult.Failure(
                CapabilityFailure(
                    code = CapabilityFailure.FailureCode.UNKNOWN,
                    message = e.message ?: "Failed to launch URI '$uri'"
                )
            )
        }
    }
}
