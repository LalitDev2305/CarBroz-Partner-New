package com.carbroz.partner.sdui.host.model

import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen

public sealed interface SduiHostState {
    public object Loading : SduiHostState
    public data class Content(
        val assembledScreen: AssembledSduiScreen,
        val isRefreshing: Boolean = false
    ) : SduiHostState
    public data class Error(
        val message: String
    ) : SduiHostState
}
