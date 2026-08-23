package com.carbroz.foundation.configuration.featureflag

/**
 * Immutable view of feature-flag state captured at one logical point in time.
 *
 * Consumers that need consistency across a render/navigation decision should
 * use a snapshot instead of resolving flags one-by-one from a mutable source.
 */
data class FeatureFlagSnapshot(
    private val states: Map<FeatureFlag, FeatureFlagState>,
) {
    fun state(flag: FeatureFlag): FeatureFlagState = states[flag] ?: FeatureFlagState.Unknown
}

/** Captures a consistent feature-flag snapshot for the requested flags. */
fun interface FeatureFlagSnapshotProvider {
    fun snapshot(flags: Set<FeatureFlag>): FeatureFlagSnapshot
}
