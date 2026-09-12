package com.carbroz.feature.dynamic

import com.carbroz.sdui.render.SduiInteraction

sealed interface DynamicScreenIntent {
    data class Show(val destination: DynamicDestination) : DynamicScreenIntent
    data object Retry : DynamicScreenIntent
    data object Refresh : DynamicScreenIntent
    data class Interaction(val value: SduiInteraction) : DynamicScreenIntent
    data object BackRequested : DynamicScreenIntent
}
