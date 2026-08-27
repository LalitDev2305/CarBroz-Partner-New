package com.carbroz.runtime.sdui

import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.SectionContent
import com.carbroz.runtime.sdui.model.elements
import com.carbroz.runtime.sdui.normalization.SduiNormalizationResult
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
import com.carbroz.runtime.sdui.protocol.ComponentDto
import com.carbroz.runtime.sdui.protocol.ElementDto
import com.carbroz.runtime.sdui.protocol.GroupDto
import com.carbroz.runtime.sdui.protocol.ScreenDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.protocol.SduiValidationResult
import com.carbroz.runtime.sdui.protocol.SduiViolationCode
import com.carbroz.runtime.sdui.protocol.SectionDto
import com.carbroz.runtime.sdui.protocol.TemplateDto
import com.carbroz.runtime.sdui.registry.SduiRegistryFactory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FlexibleHierarchyTest {
    @Test
    fun oneTemplateSupportsComponentsWithDifferentDepthsSimultaneously() {
        val envelope = mixedDepthEnvelope()
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))

        val screen = assertIs<SduiNormalizationResult.Success>(
            SduiNormalizer(SduiRegistryFactory.createCore()).normalize(envelope),
        ).screen

        assertEquals(3, screen.template.components.size)
        assertIs<ComponentContent.Elements>(screen.template.components[0].content)

        val componentB = assertIs<ComponentContent.Sections>(screen.template.components[1].content)
        assertEquals(2, componentB.values.size)
        assertIs<SectionContent.Elements>(componentB.values[0].content)
        assertIs<SectionContent.Elements>(componentB.values[1].content)

        val componentC = assertIs<ComponentContent.Sections>(screen.template.components[2].content)
        val groups = assertIs<SectionContent.Groups>(componentC.values.single().content)
        assertEquals(2, groups.values.size)
        assertEquals(9, screen.elements().count())
    }

    @Test
    fun componentCannotMixDirectElementsAndSections() {
        val component = ComponentDto(
            id = "mixed",
            type = "STACK",
            elements = listOf(text("direct", "Direct")),
            sections = listOf(SectionDto(id = "section", type = "STACK", elements = listOf(text("nested", "Nested")))),
        )
        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope(component)))
        assertEquals(SduiViolationCode.ConflictingBranchContent, invalid.violations.single().code)
    }

    @Test
    fun sectionCannotMixDirectElementsAndGroups() {
        val section = SectionDto(
            id = "mixed-section",
            type = "STACK",
            elements = listOf(text("direct", "Direct")),
            groups = listOf(GroupDto(id = "group", type = "STACK", elements = listOf(text("nested", "Nested")))),
        )
        val invalid = assertIs<SduiValidationResult.Invalid>(
            SduiSchemaValidator().validate(
                envelope(ComponentDto(id = "component", type = "STACK", sections = listOf(section))),
            ),
        )
        assertEquals(SduiViolationCode.ConflictingBranchContent, invalid.violations.single().code)
    }

    @Test
    fun structuralLevelsCannotBeEmpty() {
        val invalid = assertIs<SduiValidationResult.Invalid>(
            SduiSchemaValidator().validate(envelope(ComponentDto(id = "empty", type = "STACK"))),
        )
        assertEquals(SduiViolationCode.EmptyBranch, invalid.violations.single().code)
    }

    private fun mixedDepthEnvelope(): SduiEnvelopeDto = envelope(
        ComponentDto(
            id = "component-a",
            type = "STACK",
            elements = listOf(text("logo", "Logo"), text("title", "Title")),
        ),
        ComponentDto(
            id = "component-b",
            type = "STACK",
            sections = listOf(
                SectionDto(
                    id = "phone-section",
                    type = "STACK",
                    elements = listOf(input("phone", "phone_number"), button("continue", "Continue")),
                ),
                SectionDto(
                    id = "helper-section",
                    type = "STACK",
                    elements = listOf(text("helper", "Helper")),
                ),
            ),
        ),
        ComponentDto(
            id = "component-c",
            type = "STACK",
            sections = listOf(
                SectionDto(
                    id = "terms-section",
                    type = "STACK",
                    groups = listOf(
                        GroupDto(
                            id = "group-a",
                            type = "STACK",
                            elements = listOf(text("terms-a", "Terms A"), text("terms-b", "Terms B")),
                        ),
                        GroupDto(
                            id = "group-b",
                            type = "STACK",
                            elements = listOf(text("terms-c", "Terms C"), button("agree", "Agree")),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun envelope(vararg components: ComponentDto) = SduiEnvelopeDto(
        protocolVersion = 1,
        schemaVersion = 1,
        screen = ScreenDto(
            id = "screen",
            version = 1,
            template = TemplateDto(
                id = "login",
                type = "FORM_TEMPLATE",
                components = components.toList(),
            ),
        ),
    )

    private fun text(id: String, value: String) = ElementDto(
        id = id,
        type = "TEXT",
        properties = JsonObject(mapOf("text" to JsonPrimitive(value))),
    )

    private fun input(id: String, fieldId: String) = ElementDto(
        id = id,
        type = "INPUT",
        properties = JsonObject(mapOf("fieldId" to JsonPrimitive(fieldId))),
    )

    private fun button(id: String, value: String) = ElementDto(
        id = id,
        type = "BUTTON",
        properties = JsonObject(mapOf("text" to JsonPrimitive(value))),
    )
}
