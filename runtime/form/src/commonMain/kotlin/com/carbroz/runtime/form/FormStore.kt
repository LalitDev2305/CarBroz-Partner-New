package com.carbroz.runtime.form

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonElement

class FormStore(definitions: List<FormFieldDefinition>) {
    private val definitionsById: Map<FormFieldId, FormFieldDefinition> = definitions.associateBy { it.id }.also {
        require(it.size == definitions.size) { "Duplicate form field id" }
    }

    private val initialState = FormState(
        fields = definitions.associate { definition ->
            definition.id to FormFieldState(
                id = definition.id,
                value = definition.initialValue,
                initialValue = definition.initialValue,
            )
        },
    )

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<FormState> = mutableState.asStateFlow()

    fun update(id: FormFieldId, value: JsonElement, touch: Boolean = true): Boolean {
        val definition = definitionsById[id] ?: return false
        mutableState.update { current ->
            val existing = current.fields.getValue(id)
            val candidate = existing.copy(value = value, touched = existing.touched || touch)
            val provisional = current.copy(fields = current.fields + (id to candidate))
            val validated = candidate.copy(
                errors = definition.validators.flatMap { it.validate(value, provisional) },
            )
            provisional.copy(fields = provisional.fields + (id to validated))
        }
        return true
    }

    fun touch(id: FormFieldId): Boolean {
        if (id !in definitionsById) return false
        mutableState.update { current ->
            val existing = current.fields.getValue(id)
            current.copy(fields = current.fields + (id to existing.copy(touched = true)))
        }
        return true
    }

    fun validate(): Boolean {
        mutableState.update { current ->
            val validatedFields = current.fields.mapValues { (id, field) ->
                val definition = definitionsById.getValue(id)
                field.copy(errors = definition.validators.flatMap { it.validate(field.value, current) })
            }
            current.copy(fields = validatedFields, submitAttempts = current.submitAttempts + 1)
        }
        return state.value.valid
    }

    fun reset() {
        mutableState.value = initialState
    }
}
