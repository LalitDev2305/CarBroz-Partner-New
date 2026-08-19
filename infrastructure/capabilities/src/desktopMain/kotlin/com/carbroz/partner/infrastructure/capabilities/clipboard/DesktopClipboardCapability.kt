package com.carbroz.partner.infrastructure.capabilities.clipboard

import com.carbroz.partner.domain.capabilities.clipboard.ClipboardGateway
import com.carbroz.partner.domain.capabilities.core.CapabilityFailure
import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop AWT implementation of [ClipboardGateway].
 *
 * Copies plain text to system clipboard via java.awt.Toolkit.
 */
public class DesktopClipboardCapability : ClipboardGateway {

    override suspend fun copyText(text: String): CapabilityResult<Unit> {
        return try {
            val selection = StringSelection(text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
            CapabilityResult.Success(Unit)
        } catch (e: Exception) {
            CapabilityResult.Failure(
                CapabilityFailure(
                    code = CapabilityFailure.FailureCode.UNKNOWN,
                    message = e.message ?: "Failed to copy text to clipboard"
                )
            )
        }
    }
}
