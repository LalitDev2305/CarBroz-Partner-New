package com.carbroz.runtime.action

import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.sdui.model.CapabilityCommand
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CapabilityActionDefinitionTest {
    private val context = ActionPreparationContext(bindings = BindingContext(emptyMap()))

    @Test
    fun coreRegistryPreparesCapabilityCommand() {
        val registry = ActionRegistry.builder().registerAll(CoreActionDefinitions.all).build()
        val preparer = ActionPreparer(registry)

        val result = preparer.prepare(
            CapabilityCommand(
                capability = "external_uri",
                operation = "open",
                arguments = mapOf("uri" to JsonPrimitive("https://example.com")),
            ),
            context,
        )

        val success = assertIs<ActionPreparationResult.Success>(result)
        val action = assertIs<PreparedAction.Capability>(success.action)
        assertEquals(CapabilityKind.EXTERNAL_URI, action.request.kind)
        assertEquals("open", action.request.operation)
    }

    @Test
    fun unknownCapabilityIsRejectedByDefinition() {
        val registry = ActionRegistry.builder().registerAll(CoreActionDefinitions.all).build()
        val preparer = ActionPreparer(registry)

        val result = preparer.prepare(
            CapabilityCommand(capability = "teleport", operation = "start"),
            context,
        )

        assertIs<ActionPreparationResult.DefinitionRejectedCommand>(result)
    }
}
