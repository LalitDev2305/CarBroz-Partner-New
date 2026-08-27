package com.carbroz.runtime.sdui.registry

import com.carbroz.runtime.sdui.component.ComponentDefinitions
import com.carbroz.runtime.sdui.element.ElementDefinitions
import com.carbroz.runtime.sdui.element.button.ButtonElementDefinition
import com.carbroz.runtime.sdui.element.button.ButtonElementProperties
import com.carbroz.runtime.sdui.element.text.TextElementDefinition
import com.carbroz.runtime.sdui.element.text.TextElementProperties
import com.carbroz.runtime.sdui.element.text.TextStyleToken
import com.carbroz.runtime.sdui.extension.ElementDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.group.GroupDefinitions
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.section.SectionDefinitions
import com.carbroz.runtime.sdui.template.TemplateDefinitions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SduiRegistryFactoryTest {
    @Test
    fun coreRegistryComposesOnlyLevelOwnedCollections() {
        val registry = SduiRegistryFactory.createCore()
        val expectedSize = TemplateDefinitions.all.size + ComponentDefinitions.all.size +
            SectionDefinitions.all.size + GroupDefinitions.all.size + ElementDefinitions.all.size

        assertEquals(expectedSize, registry.size)
        assertTrue(registry.supports(NodeKind.TEMPLATE, NodeType("FORM_TEMPLATE")))
        assertTrue(registry.supports(NodeKind.COMPONENT, NodeType("STACK")))
        assertTrue(registry.supports(NodeKind.SECTION, NodeType("STACK")))
        assertTrue(registry.supports(NodeKind.GROUP, NodeType("STACK")))
        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("TEXT")))
        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("BUTTON")))
        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("INPUT")))
    }

    @Test
    fun newElementDefinitionRequiresNoRendererOrNavigationChanges() {
        val registry = SduiRegistryBuilder().apply {
            registerAll(TemplateDefinitions.all)
            registerAll(ComponentDefinitions.all)
            registerAll(SectionDefinitions.all)
            registerAll(GroupDefinitions.all)
            registerAll(ElementDefinitions.all)
            register(RatingElementDefinition)
        }.build()

        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("RATING")))
        assertEquals(RatingElementDefinition, registry.find(NodeKind.ELEMENT, NodeType("RATING")))
        val decoded = RatingElementDefinition.decodeProperties(JsonObject(mapOf("max" to JsonPrimitive(5))))
        assertEquals(5, assertIs<PropertyDecodeResult.Success<RatingProperties>>(decoded).properties.max)
    }

    @Test
    fun textDefinitionDecodesTypedCommonAndSpecificProperties() {
        val result = TextElementDefinition.decodeProperties(
            JsonObject(
                mapOf(
                    "text" to JsonPrimitive("Welcome"),
                    "style" to JsonPrimitive("TITLE_LARGE"),
                    "padding" to JsonPrimitive(12),
                ),
            ),
        )

        val properties = assertIs<PropertyDecodeResult.Success<TextElementProperties>>(result).properties
        assertEquals("Welcome", properties.text)
        assertEquals(TextStyleToken.TITLE_LARGE, properties.style)
        assertEquals(12f, properties.common.padding.start)
    }

    @Test
    fun buttonPreservesFullWidthDefault() {
        val result = ButtonElementDefinition.decodeProperties(
            JsonObject(mapOf("text" to JsonPrimitive("Continue"))),
        )
        val properties = assertIs<PropertyDecodeResult.Success<ButtonElementProperties>>(result).properties
        assertTrue(properties.common.fillWidth)
    }

    private data class RatingProperties(val max: Int) : NodeProperties

    private object RatingElementDefinition : ElementDefinition<RatingProperties> {
        override val type: NodeType = NodeType("RATING")
        override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<RatingProperties> {
            val max = (raw["max"] as? JsonPrimitive)?.intOrNull
                ?: return PropertyDecodeResult.Failure("RATING requires integer 'max'")
            return PropertyDecodeResult.Success(RatingProperties(max))
        }
    }
}
