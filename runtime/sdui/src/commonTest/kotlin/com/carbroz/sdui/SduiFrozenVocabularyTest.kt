package com.carbroz.sdui

import com.carbroz.sdui.fixtures.SduiFrozenContractFixtures
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.parser.SduiDecodeResult
import com.carbroz.sdui.parser.SduiDecoder
import com.carbroz.sdui.parser.SduiSupportChecker
import com.carbroz.sdui.parser.SduiSupportResult
import com.carbroz.sdui.registry.SduiNodeRegistration
import com.carbroz.sdui.value.SduiExecutionContext
import com.carbroz.sdui.value.SduiValueResolution
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SduiFrozenVocabularyTest {
    private val decoder = SduiDecoder()
    private val checker = SduiSupportChecker(SduiNodeRegistration.createRegistry())

    @Test
    fun canonicalProductFixturesDecodeAndUseOnlyRegisteredVocabulary() {
        listOf(
            SduiFrozenContractFixtures.login,
            SduiFrozenContractFixtures.otp,
            SduiFrozenContractFixtures.dashboard,
            SduiFrozenContractFixtures.bookingDetails,
            SduiFrozenContractFixtures.allNodeTypes,
            SduiFrozenContractFixtures.allActionTypes,
        ).forEach { raw ->
            val decoded = assertIs<SduiDecodeResult.Success>(decoder.decode(Json.parseToJsonElement(raw)))
            assertIs<SduiSupportResult.Supported>(checker.check(decoded.screen))
        }
    }

    @Test
    fun allNodeFixtureCoversEveryRegisteredHierarchyAndElement() {
        val screen = assertIs<SduiDecodeResult.Success>(
            decoder.decode(Json.parseToJsonElement(SduiFrozenContractFixtures.allNodeTypes)),
        ).screen
        val registry = SduiNodeRegistration.createRegistry()

        assertEquals("stack_template", screen.template.type)
        assertEquals("stack_component", screen.template.components.single().type)
        val component = screen.template.components.single()
        component.elements.orEmpty().forEach { assertEquals(true, registry.supportsElement(it.type)) }
        component.sections.orEmpty().forEach { section ->
            assertEquals(true, registry.supportsSection(section.type))
            section.elements.orEmpty().forEach { assertEquals(true, registry.supportsElement(it.type)) }
            section.groups.orEmpty().forEach { group ->
                assertEquals(true, registry.supportsGroup(group.type))
                group.elements.forEach { assertEquals(true, registry.supportsElement(it.type)) }
            }
        }

        val elements = component.elements.orEmpty()
        val text = elements.single { it.type == "text" }
        assertIs<JsonArray>(text.properties["spans"])
        assertIs<JsonObject>(text.properties["leading"])
        assertIs<JsonObject>(elements.single { it.type == "input" }.properties["leading"])
        assertIs<JsonObject>(elements.single { it.type == "button" }.properties["trailing"])
    }

    @Test
    fun componentAndSectionPreserveBothAllowedChildBranches() {
        val screen = assertIs<SduiDecodeResult.Success>(
            decoder.decode(Json.parseToJsonElement(SduiFrozenContractFixtures.allNodeTypes)),
        ).screen
        val component = screen.template.components.single()
        val section = component.sections.orEmpty().single()

        assertTrue(component.elements.orEmpty().isNotEmpty())
        assertTrue(component.sections.orEmpty().isNotEmpty())
        assertTrue(section.elements.orEmpty().isNotEmpty())
        assertTrue(section.groups.orEmpty().isNotEmpty())
        assertIs<SduiSupportResult.Supported>(checker.check(screen))
    }

    @Test
    fun allActionFixtureDecodesExactlyTheSevenFrozenWireActions() {
        val screen = assertIs<SduiDecodeResult.Success>(
            decoder.decode(Json.parseToJsonElement(SduiFrozenContractFixtures.allActionTypes)),
        ).screen
        val actions = screen.template.components.single().elements.orEmpty().single().actions.values

        assertEquals(7, actions.size)
        assertEquals(1, actions.count { it is SduiAction.Request })
        assertEquals(1, actions.count { it is SduiAction.Navigate })
        assertEquals(1, actions.count { it is SduiAction.Present })
        assertEquals(1, actions.count { it is SduiAction.Dismiss })
        assertEquals(1, actions.count { it is SduiAction.State })
        assertEquals(1, actions.count { it is SduiAction.ExternalUri })
        assertEquals(1, actions.count { it is SduiAction.Sequence })
    }

    @Test
    fun allValueReferenceFixtureResolvesRecursively() {
        val resolver = SduiValueResolver()
        val input = Json.parseToJsonElement(SduiFrozenContractFixtures.allValueReferences)
        val result = assertIs<SduiValueResolution.Success>(
            resolver.resolve(
                input,
                SduiExecutionContext(
                    bindings = mapOf("phone" to JsonPrimitive("9999999999")),
                    context = JsonObject(
                        mapOf("authFlow" to JsonObject(mapOf("phoneNumber" to JsonPrimitive("9999999999")))),
                    ),
                    response = JsonObject(
                        mapOf("data" to JsonObject(mapOf("challengeId" to JsonPrimitive("challenge-new")))),
                    ),
                ),
            ),
        )
        val resolved = assertIs<JsonObject>(result.value)

        assertEquals(JsonPrimitive("9999999999"), resolved["binding"])
        assertEquals(JsonPrimitive("9999999999"), resolved["context"])
        assertEquals(JsonPrimitive("challenge-new"), resolved["response"])
        assertIs<JsonObject>(resolved["literal"])
    }

    @Test
    fun unsupportedVocabularyFailsAtCapabilityBoundaryInsteadOfGuessing() {
        val screen = assertIs<SduiDecodeResult.Success>(
            decoder.decode(Json.parseToJsonElement(SduiFrozenContractFixtures.unsupportedVocabulary)),
        ).screen

        assertEquals(
            SduiSupportResult.Unsupported("unsupported_template:unknown_template"),
            checker.check(screen),
        )
    }

    @Test
    fun unsupportedEmbeddedTextSpanActionFailsAtCapabilityBoundary() {
        val screen = assertIs<SduiDecodeResult.Success>(
            decoder.decode(Json.parseToJsonElement(SduiFrozenContractFixtures.unsupportedSpanAction)),
        ).screen

        assertEquals(
            SduiSupportResult.Unsupported("unsupported_action"),
            checker.check(screen),
        )
    }
}
