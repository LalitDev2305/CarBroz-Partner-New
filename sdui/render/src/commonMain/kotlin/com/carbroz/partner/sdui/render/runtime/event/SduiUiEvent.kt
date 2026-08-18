package com.carbroz.partner.sdui.render.runtime.event

import com.carbroz.partner.sdui.engine.model.SduiAction
import com.carbroz.partner.sdui.engine.model.SduiBackPolicy
import com.carbroz.partner.sdui.engine.model.SduiNodeRef
import com.carbroz.partner.sdui.engine.model.SduiParentAction
import com.carbroz.partner.sdui.engine.model.SduiRefreshPolicy

public sealed interface SduiUiEvent {
    public data class NodeTriggered(
        val node: SduiNodeRef,
        val action: SduiAction?,
        val parentAction: SduiParentAction?
    ) : SduiUiEvent

    public data class InputChanged(
        val nodeId: String,
        val newValue: String
    ) : SduiUiEvent

    public data class BackRequested(
        val policy: SduiBackPolicy?
    ) : SduiUiEvent

    public data class RefreshRequested(
        val policy: SduiRefreshPolicy?
    ) : SduiUiEvent
}

public fun interface SduiUiEventSink {
    public fun onUiEvent(event: SduiUiEvent)
}
