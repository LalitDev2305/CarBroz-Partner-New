package com.carbroz.partner.sdui.engine.assembly

import com.carbroz.partner.sdui.engine.model.SduiNodeRef
import com.carbroz.partner.sdui.engine.model.SduiScreen

public data class AssembledSduiScreen(
    val screen: SduiScreen,
    val nodeIndex: Map<String, SduiNodeRef>
)
