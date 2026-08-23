package com.carbroz.foundation.navigation

/**
 * Product-neutral deep-link input. The platform host supplies the raw URI
 * string; parsing and allow-listing remain inside navigation-owned policy.
 */
data class DeepLinkRequest(
    val uri: String,
)

/** Result of resolving a deep-link request into semantic navigation. */
sealed interface DeepLinkResolution {
    data class Resolved(val command: NavigationCommand) : DeepLinkResolution
    data class Rejected(val reason: DeepLinkRejectionReason) : DeepLinkResolution
}

enum class DeepLinkRejectionReason {
    BlankUri,
    Unsupported,
}

/**
 * Resolves trusted application links into semantic navigation commands without
 * exposing Navigation 3 or platform URI types to callers.
 */
fun interface DeepLinkResolver {
    fun resolve(request: DeepLinkRequest): DeepLinkResolution
}
