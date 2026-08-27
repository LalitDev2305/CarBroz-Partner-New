package com.carbroz.runtime.sdui.template.form.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonElement

class FormStore(definitions: List<FormFieldDefinition>) {
    private val definitionsById: Map<FormFieldId, FormFieldDefinition> = definitions.associateBy { it.id }.also {
        require(it.size == definitions.size) { "Duplicate form field id" }
    }

    private val initialState = applyRules(
        FormState(
            fields = definitions.associate { definition ->
                definition.id to FormFieldState(
                    id = definition.id,
                    value = definition.initialValue,
                    initialValue = definition.initialValue,
                )
            },
        ),
    )

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<FormState> = mutableState.asStateFlow()

    fun update(id: FormFieldId, value: JsonElement, touch: Boolean = true): Boolean {
        if (id !in definitionsById) return false
        mutableState.update { current ->
            val existing = current.fields.getValue(id)
            val candidate = existing.copy(value = value, touched = existing.touched || touch)
            val provisional = current.copy(
                fields = current.fields + (id to candidate),
                submissionError = null,
            )
            validateSync(applyRules(provisional))
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
            validateSync(applyRules(current)).copy(submitAttempts = current.submitAttempts + 1)
        }
        return state.value.valid
    }

    suspend fun validateAsync(): Boolean {
        val syncValid = validate()
        if (!syncValid) return false

        for ((id, definition) in definitionsById) {
            val currentField = state.value.fields.getValue(id)
            if (!currentField.visible || !currentField.enabled || definition.asyncValidators.isEmpty()) continue

            mutableState.update { current ->
                current.copy(fields = current.fields + (id to current.fields.getValue(id).copy(validating = true)))
            }
            val snapshot = state.value
            val asyncErrors = definition.asyncValidators.flatMap { validator ->
                validator.validate(snapshot.fields.getValue(id).value, snapshot)
            }
            mutableState.update { current ->
                val field = current.fields.getValue(id)
                current.copy(
                    fields = current.fields + (id to field.copy(validating = false, errors = field.errors + asyncErrors)),
                )
            }
        }
        return state.value.valid
    }

    fun setServerErrors(id: FormFieldId, errors: List<FormValidationError>): Boolean {
        if (id !in definitionsById) return false
        mutableState.update { current ->
            val field = current.fields.getValue(id)
            current.copy(fields = current.fields + (id to field.copy(errors = errors, touched = true)))
        }
        return true
    }

    fun beginSubmission(): Boolean {
        if (state.value.submitting) return false
        mutableState.update { it.copy(submitting = true, submissionError = null) }
        return true
    }

    fun endSubmission(error: String? = null) {
        mutableState.update { it.copy(submitting = false, submissionError = error) }
    }

    fun restore(snapshot: FormState): Boolean {
        if (snapshot.fields.keys != definitionsById.keys) return false
        mutableState.value = validateSync(applyRules(snapshot.copy(submitting = false)))
        return true
    }

    fun reset() {
        mutableState.value = initialState
    }

    private fun validateSync(state: FormState): FormState {
        val fields = state.fields.mapValues { (id, field) ->
            val definition = definitionsById.getValue(id)
            if (!field.visible || !field.enabled) {
                field.copy(errors = emptyList(), validating = false)
            } else {
                field.copy(errors = definition.validators.flatMap { it.validate(field.value, state) })
            }
        }
        return applyRules(state.copy(fields = fields))
    }

    private fun applyRules(state: FormState): FormState {
        if (state.fields.isEmpty()) return state
        val fields = state.fields.mapValues { (id, field) ->
            val definition = definitionsById.getValue(id)
            field.copy(
                visible = definition.visibleWhen.evaluate(state),
                enabled = definition.enabledWhen.evaluate(state),
            )
        }
        return state.copy(fields = fields)
    }
}
