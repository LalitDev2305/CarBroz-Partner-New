package com.carbroz.partner.sdui.runtime.state

import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public data class SduiScreenState(
    val assembledScreen: AssembledSduiScreen,
    val overlay: SduiNodeOverlay = SduiNodeOverlay()
) {
    public val snapshot: SduiRenderSnapshot
        get() = SduiRenderSnapshot(
            inputValues = overlay.inputValues,
            executingNodeIds = overlay.executingNodeIds,
            nodeVisibility = overlay.nodeVisibility,
            nodeEnabled = overlay.nodeEnabled,
            parentSignalVersions = overlay.parentSignalVersions
        )
}
