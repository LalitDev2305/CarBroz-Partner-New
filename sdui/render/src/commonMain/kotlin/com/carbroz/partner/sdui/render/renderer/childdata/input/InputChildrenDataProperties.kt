package com.carbroz.partner.sdui.render.renderer.childdata.input

import com.carbroz.partner.sdui.engine.model.SduiValidationRules
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

public data class InputChildrenDataProperties(
    val placeholder: String = "",
    val label: String = "",
    val validationRules: SduiValidationRules = SduiValidationRules()
) {
    public companion object {
        public fun decode(jsonObject: JsonObject?): InputChildrenDataProperties {
            if (jsonObject == null) return InputChildrenDataProperties()
            val placeholderVal = jsonObject["placeholder"]?.jsonPrimitive?.content ?: ""
            val labelVal = jsonObject["label"]?.jsonPrimitive?.content ?: ""
            val requiredVal = jsonObject["required"]?.jsonPrimitive?.booleanOrNull ?: false
            val regexVal = jsonObject["regex"]?.jsonPrimitive?.content
            val minLengthVal = jsonObject["min_length"]?.jsonPrimitive?.intOrNull
            val maxLengthVal = jsonObject["max_length"]?.jsonPrimitive?.intOrNull
            val errorMessageVal = jsonObject["error_message"]?.jsonPrimitive?.content

            val rules = SduiValidationRules(
                required = requiredVal,
                regex = regexVal,
                minLength = minLengthVal,
                maxLength = maxLengthVal,
                errorMessage = errorMessageVal
            )

            return InputChildrenDataProperties(
                placeholder = placeholderVal,
                label = labelVal,
                validationRules = rules
            )
        }
    }
}
