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

/** Canonical core action preparation assembly owned by runtime:action. */
object ActionPreparerFactory {
    fun createCore(): ActionPreparer = ActionPreparer(
        ActionRegistry.builder()
            .registerAll(CoreActionDefinitions.all)
            .build(),
    )
}
