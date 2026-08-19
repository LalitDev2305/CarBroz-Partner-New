package com.carbroz.partner.sdui.runtime.handler

import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen
import com.carbroz.partner.sdui.engine.model.SduiParentAction
import com.carbroz.partner.sdui.runtime.state.SduiNodeOverlay

public class ParentActionHandler {

    public fun handle(
        parentAction: SduiParentAction,
        assembledScreen: AssembledSduiScreen,
        overlay: SduiNodeOverlay
    ): SduiNodeOverlay {
        val targetNodeRef = assembledScreen.nodeIndex[parentAction.targetId] ?: return overlay
        if (!targetNodeRef.acceptsParentAction) return overlay

        val currentVersion = overlay.parentSignalVersions[parentAction.targetId] ?: 0L
        val updatedVersions = overlay.parentSignalVersions + (parentAction.targetId to (currentVersion + 1L))
        return overlay.copy(parentSignalVersions = updatedVersions)
    }
}
