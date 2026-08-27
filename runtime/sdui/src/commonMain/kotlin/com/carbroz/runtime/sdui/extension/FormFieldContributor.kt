package com.carbroz.runtime.sdui.extension

import com.carbroz.runtime.sdui.model.NodeProperties
import kotlinx.serialization.json.JsonElement

data class FormFieldContribution(
    val fieldId: String,
    val initialValue: JsonElement,
    val required: Boolean = false,
)

/** Optional capability for Element definitions that contribute generic form state. */
interface FormFieldContributor {
    fun formFieldContribution(properties: NodeProperties): FormFieldContribution?
}
