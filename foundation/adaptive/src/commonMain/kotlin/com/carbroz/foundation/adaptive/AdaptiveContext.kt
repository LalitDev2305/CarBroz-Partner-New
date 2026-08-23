package com.carbroz.foundation.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpRect

/**
 * Product-neutral posture supplied by an outer platform adapter when the
 * platform can report a meaningful folding posture.
 */
enum class AdaptivePosture {
    Normal,
    Book,
    Tabletop,
}

/** Primary interaction mode that can influence adaptive presentation choices. */
enum class AdaptiveInputMode {
    Touch,
    Pointer,
    Keyboard,
    Mixed,
}

/**
 * A non-content region inside the current adaptive container, such as a hinge.
 * Coordinates are expressed in the same Compose coordinate space as the
 * container and remain free of platform/window-manager types.
 */
@Immutable
data class AdaptiveOcclusion(
    val bounds: DpRect,
)

/**
 * Semantic environment information that cannot be inferred from width/height
 * alone. Platform hosts/adapters translate native posture and input APIs into
 * this neutral model; adaptive consumers never depend on those native APIs.
 */
@Immutable
data class AdaptiveContext(
    val posture: AdaptivePosture = AdaptivePosture.Normal,
    val inputMode: AdaptiveInputMode = AdaptiveInputMode.Touch,
    val occlusions: List<AdaptiveOcclusion> = emptyList(),
)
