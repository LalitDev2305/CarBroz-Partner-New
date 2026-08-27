package com.carbroz.runtime.action

import com.carbroz.runtime.action.request.RequestActionDefinition

object CoreActionDefinitions {
    val all: List<ActionDefinition> = listOf(
        RequestActionDefinition(),
        CapabilityActionDefinition(),
        NavigationActionDefinition(),
        PresentationActionDefinition(),
        LocalStateActionDefinition(),
        FormActionDefinition(),
        BackgroundActionDefinition(),
    )
}
