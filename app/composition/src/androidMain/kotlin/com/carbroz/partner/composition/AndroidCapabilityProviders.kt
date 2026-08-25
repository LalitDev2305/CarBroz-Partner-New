package com.carbroz.partner.composition

import android.content.Context
import android.content.Intent
import android.net.Uri
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

internal fun androidCapabilityProviders(context: Context): List<CapabilityProvider> = listOf(
    AndroidExternalUriProvider(context),
    AndroidShareProvider(context),
    AndroidMapsProvider(context),
    StaticCapabilityProvider(
        CapabilityKind.PERMISSION,
        CapabilityAvailability.Unavailable("Permission requests require a foreground Android host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.LOCATION,
        CapabilityAvailability.Unavailable("Location requires a foreground location provider host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.TRACKING,
        CapabilityAvailability.Unavailable("Tracking requires an operation-specific foreground/background owner"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.CAMERA,
        CapabilityAvailability.Unavailable("Camera capture requires a foreground Android activity result host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.MEDIA,
        CapabilityAvailability.Unavailable("Media picking requires a foreground Android activity result host"),
    ),
    StaticCapabilityProvider(
        CapabilityKind.NOTIFICATIONS,
        CapabilityAvailability.Unavailable("Notification permission/presentation requires Android host integration"),
    ),
)

private class AndroidExternalUriProvider(
    private val context: Context,
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
        return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
}

private class AndroidShareProvider(
    private val context: Context,
) : CapabilityProvider {
    override val kind: CapabilityKind = CapabilityKind.SHARING
    override val availability: StateFlow<CapabilityAvailability> =
        MutableStateFlow(CapabilityAvailability.Available)

    override suspend fun execute(request: CapabilityRequest): CapabilityResult {
        require(request.kind == kind)
        if (!request.operation.equals("share", ignoreCase = true)) {
            return CapabilityResult.Unsupported("Unsupported sharing operation '${request.operation}'")
        }
        val text = request.arguments["text"]?.jsonPrimitive?.contentOrNull
            ?: return CapabilityResult.Failure("missing_text", "Share operation requires 'text'")
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        return launch(context, Intent.createChooser(intent, null))
    }
}

private class AndroidMapsProvider(
    private val context: Context,
) : CapabilityProvider {
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
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        return launch(context, Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun launch(context: Context, intent: Intent): CapabilityResult = try {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) == null) {
        CapabilityResult.Unavailable("No Android activity can handle the capability request")
    } else {
        context.startActivity(intent)
        CapabilityResult.Success()
    }
} catch (error: Throwable) {
    CapabilityResult.Failure(
        code = "android_capability_failure",
        message = error.message ?: "Android capability execution failed",
    )
}
