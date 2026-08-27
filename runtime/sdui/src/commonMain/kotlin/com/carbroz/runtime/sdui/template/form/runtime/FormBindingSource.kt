package com.carbroz.runtime.sdui.template.form.runtime

import com.carbroz.runtime.binding.BindingValueSource
import kotlinx.serialization.json.JsonPrimitive

fun FormStore.asBindingValueSource(): BindingValueSource = BindingValueSource { path ->
    if (path.isEmpty() || path.size > 2) return@BindingValueSource null
    val field = state.value[FormFieldId(path.first())] ?: return@BindingValueSource null
    when (path.getOrNull(1)) {
        null, "value" -> field.value
        "dirty" -> JsonPrimitive(field.dirty)
        "touched" -> JsonPrimitive(field.touched)
        "valid" -> JsonPrimitive(field.valid)
        "visible" -> JsonPrimitive(field.visible)
        "enabled" -> JsonPrimitive(field.enabled)
        "validating" -> JsonPrimitive(field.validating)
        else -> null
    }
}
