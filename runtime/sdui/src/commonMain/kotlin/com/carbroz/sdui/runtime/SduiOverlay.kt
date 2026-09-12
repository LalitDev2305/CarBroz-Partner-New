package com.carbroz.sdui.runtime

import com.carbroz.sdui.model.SduiPresentationMode

data class SduiOverlay(
    val targetId: String,
    val presentation: SduiPresentationMode,
)
