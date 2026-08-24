package com.carbroz.runtime.sdui.registry

import com.carbroz.runtime.sdui.element.button.ButtonElementDefinition
import com.carbroz.runtime.sdui.element.button.ButtonElementProperties
import com.carbroz.runtime.sdui.element.text.TextElementDefinition
import com.carbroz.runtime.sdui.element.text.TextElementProperties
import com.carbroz.runtime.sdui.element.text.TextStyleToken
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoreSduiDefinitionsTest {
    @Test
    fun coreBundleRegistersWithoutCollisions() {
        val registry = SduiRegistryBuilder().apply {
            registerAll(CoreSduiDefinitions.all)
        }.build()

        assertEquals(CoreSduiDefinitions.all.size, registry.size)
        assertTrue(registry.supports(NodeKind.TEMPLATE, NodeType("FORM_TEMPLATE")))
        assertTrue(registry.supports(NodeKind.COMPONENT, NodeType("STACK")))
        assertTrue(registry.supports(NodeKind.SECTION, NodeType("STACK")))
        assertTrue(registry.supports(NodeKind.GROUP, NodeType("STACK")))
        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("TEXT")))
        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("BUTTON")))
    }

    @Test
    fun textDefinitionDecodesTypedSemanticProperties() {
        val result = TextElementDefinition.decodeProperties(
            JsonObject(
                mapOf(
                    "text" to JsonPrimitive("Welcome"),
                    "style" to JsonPrimitive("TITLE_LARGE"),
                ),
            ),
        )

        val success = assertIs<PropertyDecodeResult.Success<TextElementProperties>>(result)
        assertEquals("Welcome", success.properties.text)
        assertEquals(TextStyleToken.TITLE_LARGE, success.properties.style)
    }

    @Test
    fun buttonDefinitionRejectsMissingText() {
        val result = ButtonElementDefinition.decodeProperties(JsonObject(emptyMap()))
        assertIs<PropertyDecodeResult.Failure>(result)
    }

    @Test
    fun buttonDefinitionDefaultsToFullWidth() {
        val result = ButtonElementDefinition.decodeProperties(
            JsonObject(mapOf("text" to JsonPrimitive("Continue"))),
        )

        val success = assertIs<PropertyDecodeResult.Success<ButtonElementProperties>>(result)
        assertTrue(success.properties.fillWidth)
    }
}
