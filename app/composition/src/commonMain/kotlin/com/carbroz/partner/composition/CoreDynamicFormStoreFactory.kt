package com.carbroz.partner.composition

import com.carbroz.runtime.form.FormFieldDefinition
import com.carbroz.runtime.form.FormFieldId
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.form.FormValidationError
import com.carbroz.runtime.form.FormValidator
import com.carbroz.runtime.sdui.element.input.InputElementProperties
import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.SectionContent
import kotlinx.serialization.json.JsonPrimitive

/** Builds product-neutral form state only from registered generic INPUT elements. */
object CoreDynamicFormStoreFactory : DynamicFormStoreFactory {
    override fun create(screen: Screen): FormStore? {
        val definitions = screen.elements().mapNotNull { element ->
            val properties = element.properties as? InputElementProperties ?: return@mapNotNull null
            FormFieldDefinition(
                id = FormFieldId(properties.fieldId),
                initialValue = JsonPrimitive(properties.initialValue),
                validators = if (properties.required) listOf(requiredValidator) else emptyList(),
            )
        }.toList()
        return definitions.takeIf { it.isNotEmpty() }?.let(::FormStore)
    }

    private val requiredValidator = FormValidator { value, _ ->
        val content = (value as? JsonPrimitive)?.content.orEmpty()
        if (content.isBlank()) listOf(FormValidationError("required")) else emptyList()
    }

    private fun Screen.elements(): Sequence<Element> = sequence {
        for (component in template.components) {
            when (val content = component.content) {
                is ComponentContent.Elements -> yieldAll(content.values)
                is ComponentContent.Sections -> {
                    for (section in content.values) {
                        when (val sectionContent = section.content) {
                            is SectionContent.Elements -> yieldAll(sectionContent.values)
                            is SectionContent.Groups -> {
                                for (group in sectionContent.values) {
                                    yieldAll(group.elements)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
