package com.carbroz.runtime.sdui.registry

import com.carbroz.runtime.sdui.extension.ElementDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.EmptyNodeProperties
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SduiRegistryTest {
    @Test
    fun definitionIsResolvedByKindAndOpenType() {
        val builder = SduiRegistryBuilder()
        assertEquals(RegistrationResult.Registered, builder.register(TextDefinition))
        val registry = builder.build()

        assertTrue(registry.supports(NodeKind.ELEMENT, NodeType("TEXT")))
        assertFalse(registry.supports(NodeKind.COMPONENT, NodeType("TEXT")))
        assertEquals(TextDefinition, registry.find(NodeKind.ELEMENT, NodeType("TEXT")))
    }

    @Test
    fun duplicateDefinitionIsRejectedWithoutReplacingExistingDefinition() {
        val builder = SduiRegistryBuilder()
        builder.register(TextDefinition)

        assertIs<RegistrationResult.Duplicate>(builder.register(AnotherTextDefinition))
        val registry = builder.build()
        assertEquals(TextDefinition, registry.find(NodeKind.ELEMENT, NodeType("TEXT")))
    }

    private object TextDefinition : ElementDefinition<EmptyNodeProperties> {
        override val type = NodeType("TEXT")
        override fun decodeProperties(raw: JsonObject) = PropertyDecodeResult.Success(EmptyNodeProperties)
    }

    private object AnotherTextDefinition : ElementDefinition<EmptyNodeProperties> {
        override val type = NodeType("TEXT")
        override fun decodeProperties(raw: JsonObject) = PropertyDecodeResult.Success(EmptyNodeProperties)
    }
}
