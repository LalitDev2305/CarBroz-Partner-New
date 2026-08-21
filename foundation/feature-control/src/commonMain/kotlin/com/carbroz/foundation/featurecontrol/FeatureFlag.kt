package com.carbroz.foundation.featurecontrol

/**
 * Stable product-neutral feature flag identifier.
 *
 * Keys are explicit strings so remote providers can map their own storage model
 * without leaking Firebase/Remote Config/vendor types into foundation code.
 */
data class FeatureFlag(val key: String) {
    init {
        require(key.matches(Regex("^[a-z][a-z0-9_.-]{2,63}$"))) {
            "Feature flag key must be 3-64 lowercase characters using letters, digits, dot, underscore, or dash."
        }
    }
}

/** Read-only feature state exposed to application and product layers. */
sealed interface FeatureFlagState {
    data object Enabled : FeatureFlagState
    data object Disabled : FeatureFlagState
    data object Unknown : FeatureFlagState
}

/**
 * Resolves the current state of a feature flag.
 *
 * Implementations may be local, remote, cached, or composite. Consumers must
 * handle [FeatureFlagState.Unknown] explicitly rather than assuming remote data
 * always exists.
 */
fun interface FeatureFlagProvider {
    fun state(flag: FeatureFlag): FeatureFlagState
}
