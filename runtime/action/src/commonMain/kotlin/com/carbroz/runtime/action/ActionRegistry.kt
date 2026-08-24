package com.carbroz.runtime.action

import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind

interface ActionDefinition {
    val kind: CommandKind

    /** Returns null only when the command instance does not match this definition's trusted model. */
    fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult?
}

class ActionRegistry private constructor(
    private val definitions: Map<CommandKind, ActionDefinition>,
) {
    val size: Int get() = definitions.size

    fun find(kind: CommandKind): ActionDefinition? = definitions[kind]

    fun supports(kind: CommandKind): Boolean = kind in definitions

    companion object {
        fun builder(): ActionRegistryBuilder = ActionRegistryBuilder()
    }
}

class ActionRegistryBuilder {
    private val definitions = linkedMapOf<CommandKind, ActionDefinition>()

    fun register(definition: ActionDefinition): ActionRegistryBuilder = apply {
        require(definitions.put(definition.kind, definition) == null) {
            "Duplicate action definition for '${definition.kind.value}'"
        }
    }

    fun registerAll(values: Iterable<ActionDefinition>): ActionRegistryBuilder = apply {
        values.forEach(::register)
    }

    fun build(): ActionRegistry = ActionRegistry(definitions.toMap())
}
