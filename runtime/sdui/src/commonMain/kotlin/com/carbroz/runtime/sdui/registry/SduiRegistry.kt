package com.carbroz.runtime.sdui.registry

import com.carbroz.runtime.sdui.extension.DefinitionKey
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType

sealed interface RegistrationResult {
    data object Registered : RegistrationResult
    data class Duplicate(val key: DefinitionKey) : RegistrationResult
}

class SduiRegistry internal constructor(
    private val definitions: Map<DefinitionKey, SduiDefinition<*>>,
) {
    fun find(kind: NodeKind, type: NodeType): SduiDefinition<*>? =
        definitions[DefinitionKey(kind, type)]

    fun supports(kind: NodeKind, type: NodeType): Boolean =
        DefinitionKey(kind, type) in definitions

    val size: Int get() = definitions.size
}

class SduiRegistryBuilder {
    private val definitions = linkedMapOf<DefinitionKey, SduiDefinition<*>>()
    private var built = false

    fun register(definition: SduiDefinition<*>): RegistrationResult {
        check(!built) { "SduiRegistryBuilder cannot be mutated after build()" }
        if (definition.key in definitions) return RegistrationResult.Duplicate(definition.key)
        definitions[definition.key] = definition
        return RegistrationResult.Registered
    }

    fun registerAll(values: Iterable<SduiDefinition<*>>) {
        values.forEach { definition ->
            when (val result = register(definition)) {
                RegistrationResult.Registered -> Unit
                is RegistrationResult.Duplicate -> error("Duplicate SDUI definition: ${result.key}")
            }
        }
    }

    fun build(): SduiRegistry {
        check(!built) { "SduiRegistryBuilder.build() may only be called once" }
        built = true
        return SduiRegistry(definitions.toMap())
    }
}
