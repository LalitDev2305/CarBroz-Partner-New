package com.carbroz.partner.composition

import com.carbroz.runtime.form.FormFieldDefinition
import com.carbroz.runtime.form.FormFieldId
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.form.FormValidationError
import com.carbroz.runtime.form.FormValidator
import com.carbroz.runtime.sdui.extension.FormFieldContributor
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.elements
import com.carbroz.runtime.sdui.registry.SduiRegistry
import kotlinx.serialization.json.JsonPrimitive

/** Product-neutral form discovery through registered Element capabilities, never concrete Element property classes. */
class CoreDynamicFormStoreFactory(
    private val registry: SduiRegistry,
) : DynamicFormStoreFactory {
    override fun create(screen: Screen): FormStore? {
        val definitions = screen.elements().mapNotNull { element ->
            val contributor = registry.find(NodeKind.ELEMENT, element.type) as? FormFieldContributor
                ?: return@mapNotNull null
            val contribution = contributor.formFieldContribution(element.properties) ?: return@mapNotNull null
            FormFieldDefinition(
                id = FormFieldId(contribution.fieldId),
                initialValue = contribution.initialValue,
                validators = if (contribution.required) listOf(requiredValidator) else emptyList(),
            )
        }.toList()

        require(definitions.map { it.id }.distinct().size == definitions.size) {
            "Duplicate form fieldId detected in normalized SDUI screen"
        }
        return definitions.takeIf { it.isNotEmpty() }?.let(::FormStore)
    }

    private val requiredValidator = FormValidator { value, _ ->
        val content = (value as? JsonPrimitive)?.content.orEmpty()
        if (content.isBlank()) listOf(FormValidationError("required")) else emptyList()
    }
}
