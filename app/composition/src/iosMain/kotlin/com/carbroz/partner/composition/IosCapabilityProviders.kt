package com.carbroz.partner.composition

import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRequest
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.capabilities.StaticCapabilityProvider
import com.carbroz.foundation.security.TrustedUriDecision
import com.carbroz.foundation.security.TrustedUriPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal fun iosCapabilityProviders(): List<CapabilityProvider> = listOf(
    IosExternalUriProvider(),
    IosMapsProvider(),
    StaticCapabilityProvider(
        CapabilityKind.PERMISSION,
        CapabilityAvailability.Unavailable("Permission requests require an iOS foreground host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.LOCATION,
        CapabilityAvailability.Unavailable("Location requires an iOS location delegate host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.TRACKING,
        CapabilityAvailability.Unavailable("Tracking requires operation-specific iOS lifecycle ownership"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.CAMERA,
        CapabilityAvailability.Unavailable("Camera capture requires an iOS presentation host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.MEDIA,
        CapabilityAvailability.Unavailable("Media picking requires an iOS presentation host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.NOTIFICATIONS,
        CapabilityAvailability.Unavailable("Notification authorization requires iOS host integration"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.SHARING,
        CapabilityAvailability.Unavailable("Sharing requires an iOS presentation host"),
    ),
)

private class IosExternalUriProvider(
    private val uriPolicy: TrustedUriPolicy = TrustedUriPolicy(),
) : CapabilityProvider {
    override val kind: CapabilityKind = CapabilityKind.EXTERNAL_URI
    override val availability: StateFlow<CapabilityAvailability> =
        MutableStateFlow(CapabilityAvailability.Available)

    override suspend fun execute(request: CapabilityRequest): CapabilityResult {
        require(request.kind == kind)
        if (!request.operation.equals("open", ignoreCase = true)) {
            return CapabilityResult.Unsupported("Unsupported external URI operation '${request.operation}'")
        }
        val uri = request.arguments["uri"]?.jsonPrimitive?.contentOrNull
            ?: return CapabilityResult.Failure("missing_uri", "External URI operation requires 'uri'")
        when (val decision = uriPolicy.evaluate(uri)) {
            TrustedUriDecision.Trusted -> Unit
            is TrustedUriDecision.Rejected -> return CapabilityResult.Restricted(decision.reason)
        }
        return open(uri)
    }
}

private class IosMapsProvider : CapabilityProvider {
    override val kind: CapabilityKind = CapabilityKind.MAPS
    override val availability: StateFlow<CapabilityAvailability> =
        MutableStateFlow(CapabilityAvailability.Available)

    override suspend fun execute(request: CapabilityRequest): CapabilityResult {
        require(request.kind == kind)
        if (!request.operation.equals("open", ignoreCase = true)) {
            return CapabilityResult.Unsupported("Unsupported maps operation '${request.operation}'")
        }
        val query = request.arguments["query"]?.jsonPrimitive?.contentOrNull
            ?: return CapabilityResult.Failure("missing_query", "Maps operation requires 'query'")
        val encoded = query.replace(" ", "%20")
        return open("https://maps.apple.com/?q=$encoded")
    }
}

private fun open(value: String): CapabilityResult {
    val url = NSURL.URLWithString(value)
        ?: return CapabilityResult.Failure("invalid_uri", "Invalid external URI")
    val application = UIApplication.sharedApplication
    if (!application.canOpenURL(url)) {
        return CapabilityResult.Unavailable("No iOS application can handle the capability request")
    }
    return try {
        application.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        CapabilityResult.Success()
    } catch (error: Throwable) {
        CapabilityResult.Failure(
            code = "ios_capability_failure",
            message = error.message ?: "iOS capability execution failed",
        )
    }
}
