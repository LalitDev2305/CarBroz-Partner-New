package com.carbroz.partner.sdui.render.renderer.childdata

import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public fun interface ChildrenDataRenderer {
    @Composable
    public fun render(
        childrenData: SduiChildrenData,
        snapshot: SduiRenderSnapshot,
        eventSink: SduiUiEventSink
    )
}
