package com.carbroz.foundation.featurecontrol

/**
 * Immutable view of feature-control state captured at one logical point in time.
 *
 * Consumers that need consistency across a render/navigation decision should use
 * a snapshot instead of resolving flags one-by-one from a mutable remote source.
 */
data class FeatureFlagSnapshot(
    private val states: Map<FeatureFlag, FeatureFlagState>,
) {
    fun state(flag: FeatureFlag): FeatureFlagState = states[flag] ?: FeatureFlagState.Unknown
}

/** Captures a consistent feature-control snapshot for the requested flags. */
fun interface FeatureFlagSnapshotProvider {
    fun snapshot(flags: Set<FeatureFlag>): FeatureFlagSnapshot
}
