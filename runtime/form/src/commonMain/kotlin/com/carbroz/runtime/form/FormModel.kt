package com.carbroz.runtime.form

import kotlinx.serialization.json.JsonElement

@kotlin.jvm.JvmInline
value class FormFieldId(val value: String) {
    init {
        require(value.isNotBlank()) { "FormFieldId cannot be blank" }
        require(value.length <= 128) { "FormFieldId is too long" }
    }
}

data class FormFieldState(
    val id: FormFieldId,
    val value: JsonElement,
    val initialValue: JsonElement = value,
    val touched: Boolean = false,
    val visible: Boolean = true,
    val enabled: Boolean = true,
    val validating: Boolean = false,
    val errors: List<FormValidationError> = emptyList(),
) {
    val dirty: Boolean get() = value != initialValue
    val valid: Boolean get() = errors.isEmpty()
}

data class FormState(
    val fields: Map<FormFieldId, FormFieldState> = emptyMap(),
    val submitAttempts: Int = 0,
    val submitting: Boolean = false,
    val submissionError: String? = null,
) {
    val dirty: Boolean get() = fields.values.any(FormFieldState::dirty)
    val valid: Boolean get() = fields.values.filter { it.visible && it.enabled }.all(FormFieldState::valid)
    val validating: Boolean get() = fields.values.any(FormFieldState::validating)

    operator fun get(id: FormFieldId): FormFieldState? = fields[id]
}

data class FormValidationError(
    val code: String,
    val message: String? = null,
)

fun interface FormValidator {
    fun validate(value: JsonElement, state: FormState): List<FormValidationError>
}

fun interface AsyncFormValidator {
    suspend fun validate(value: JsonElement, state: FormState): List<FormValidationError>
}

fun interface FormStateRule {
    fun evaluate(state: FormState): Boolean

    companion object {
        val Always = FormStateRule { true }
    }
}

data class FormFieldDefinition(
    val id: FormFieldId,
    val initialValue: JsonElement,
    val validators: List<FormValidator> = emptyList(),
    val asyncValidators: List<AsyncFormValidator> = emptyList(),
    val visibleWhen: FormStateRule = FormStateRule.Always,
    val enabledWhen: FormStateRule = FormStateRule.Always,
)
