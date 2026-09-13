package com.carbroz.sdui.parser

import com.carbroz.sdui.model.SduiScreen

sealed interface SduiDecodeResult {
    data class Success(val screen: SduiScreen) : SduiDecodeResult
    data class Failure(val reason: String) : SduiDecodeResult
}
