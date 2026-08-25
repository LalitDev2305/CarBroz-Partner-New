package com.carbroz.runtime.action

import com.carbroz.runtime.action.request.RequestActionDefinition

/** Registration bundle only; [ActionRegistry] remains the sole lookup mechanism. */
object CoreActionDefinitions {
    val all: List<ActionDefinition> = listOf(
        RequestActionDefinition(),
        CapabilityActionDefinition(),
    )
}
