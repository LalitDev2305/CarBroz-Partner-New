package com.carbroz.runtime.action

import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind

interface ActionDefinition {
    val kind: CommandKind

    /** Returns null only when the command instance does not match this definition's trusted model. */
    fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult?
}

class ActionRegistry internal constructor(
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
        require(definition.kind !in definitions) {
            "Duplicate action definition for '${definition.kind.value}'"
        }
        definitions[definition.kind] = definition
    }

    fun registerAll(values: Iterable<ActionDefinition>): ActionRegistryBuilder = apply {
        values.forEach(::register)
    }

    fun build(): ActionRegistry = ActionRegistry(definitions.toMap())
}
