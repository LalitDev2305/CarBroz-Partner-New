package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRequest
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.capabilities.StaticCapabilityProvider
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun desktopCapabilityProviders(): List<CapabilityProvider> {
    val unsupported = CapabilityKind.entries
        .filter { it != CapabilityKind.EXTERNAL_URI }
        .map { kind ->
            StaticCapabilityProvider(
                kind = kind,
                state = CapabilityAvailability.Unsupported("$kind is not supported by the Desktop host"),
            )
        }
    return listOf(DesktopExternalUriProvider()) + unsupported
}

private class DesktopExternalUriProvider : CapabilityProvider {
    override val kind: CapabilityKind = CapabilityKind.EXTERNAL_URI
    override val availability: StateFlow<CapabilityAvailability> = MutableStateFlow(
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            CapabilityAvailability.Available
        } else {
            CapabilityAvailability.Unsupported("Desktop browse action is unavailable")
        },
    )

    override suspend fun execute(request: CapabilityRequest): CapabilityResult {
        require(request.kind == kind)
        if (!request.operation.equals("open", ignoreCase = true)) {
            return CapabilityResult.Unsupported("Unsupported external URI operation '${request.operation}'")
        }
        val uri = request.arguments["uri"]?.jsonPrimitive?.contentOrNull
            ?: return CapabilityResult.Failure("missing_uri", "External URI operation requires 'uri'")
        return try {
            Desktop.getDesktop().browse(URI(uri))
            CapabilityResult.Success()
        } catch (error: Throwable) {
            CapabilityResult.Failure(
                code = "desktop_capability_failure",
                message = error.message ?: "Desktop URI launch failed",
            )
        }
    }
}
