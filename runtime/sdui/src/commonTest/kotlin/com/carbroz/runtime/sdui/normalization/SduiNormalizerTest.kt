package com.carbroz.runtime.sdui.normalization

import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.model.BackgroundCommand
import com.carbroz.runtime.sdui.model.EmptyNodeProperties
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import com.carbroz.runtime.sdui.protocol.BackgroundCommandDto
import com.carbroz.runtime.sdui.protocol.CommandDto
import com.carbroz.runtime.sdui.protocol.ComponentDto
import com.carbroz.runtime.sdui.protocol.ElementDto
import com.carbroz.runtime.sdui.protocol.RequestCommandDto
import com.carbroz.runtime.sdui.protocol.ScreenDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.protocol.SduiValidationResult
import com.carbroz.runtime.sdui.protocol.SduiViolationCode
import com.carbroz.runtime.sdui.protocol.TemplateDto
import com.carbroz.runtime.sdui.registry.SduiRegistryBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SduiNormalizerTest {
    @Test
    fun validScreenRequestNormalizesIntoTrustedDestinationContract() {
        val binding = "\$form.value"
        val envelope = envelope(
            RequestCommandDto(
                method = "post",
                endpoint = "/api/v1/action",
                screenId = "next-screen",
                templateId = "next-template",
                templateType = "FORM_TEMPLATE",
                payload = JsonObject(mapOf("value" to JsonPrimitive(binding))),
            ),
        )
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
        val command = normalizedCommand<RequestCommand>(envelope)
        val destination = assertNotNull(command.destination)
        assertEquals(RequestMethod.POST, command.method)
        assertEquals("next-screen", destination.screenId)
        assertEquals(JsonPrimitive(binding), command.payload["value"])
    }

    @Test
    fun noScreenRequestDoesNotRequireDestinationIdentity() {
        val envelope = envelope(
            RequestCommandDto(
                method = "POST",
                endpoint = "/api/v1/state",
                responseMode = "NONE",
                validateForm = false,
            ),
        )
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
        val command = normalizedCommand<RequestCommand>(envelope)
        assertEquals(RequestResponseMode.NONE, command.responseMode)
        assertEquals(null, command.destination)
    }

    @Test
    fun backgroundCommandNormalizesWithoutPlatformTypesInProtocolLayer() {
        val envelope = envelope(
            BackgroundCommandDto(
                operation = "CANCEL",
                id = "generic-task",
            ),
        )
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
        assertEquals("generic-task", normalizedCommand<BackgroundCommand>(envelope).id)
    }

    @Test
    fun validatorRejectsAbsoluteRequestEndpoint() {
        val envelope = envelope(
            RequestCommandDto(
                method = "POST",
                endpoint = "https://evil.example/action",
                screenId = "next",
                templateId = "template",
                templateType = "FORM_TEMPLATE",
            ),
        )
        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.EndpointMustBeRelative, invalid.violations.single().code)
    }

    @Test
    fun validatorRejectsUnsupportedRequestMethod() {
        val envelope = envelope(
            RequestCommandDto(
                method = "TRACE",
                endpoint = "/api/v1/action",
                screenId = "next",
                templateId = "template",
                templateType = "FORM_TEMPLATE",
            ),
        )
        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.UnsupportedRequestMethod, invalid.violations.single().code)
    }

    @Test
    fun normalizationRejectsUnknownElementDefinitionBeforeRendering() {
        val envelope = envelope(command = null, elementType = "UNKNOWN")
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
        assertIs<SduiNormalizationError.UnsupportedDefinition>(
            assertIs<SduiNormalizationResult.Failure>(SduiNormalizer(registry()).normalize(envelope)).error,
        )
    }

    @Test
    fun normalizedNodePathUsesSemanticIdsRatherThanIndexes() {
        val result = assertIs<SduiNormalizationResult.Success>(SduiNormalizer(registry()).normalize(envelope(command = null)))
        val element = (result.screen.template.components.single().content as com.carbroz.runtime.sdui.model.ComponentContent.Elements).values.single()
        assertEquals("screen/template/component/continue", element.path.toString())
    }

    private inline fun <reified T> normalizedCommand(envelope: SduiEnvelopeDto): T {
        val result = assertIs<SduiNormalizationResult.Success>(SduiNormalizer(registry()).normalize(envelope))
        val command = (result.screen.template.components.single().content as com.carbroz.runtime.sdui.model.ComponentContent.Elements)
            .values.single().command
        return assertIs<T>(command)
    }

    private fun envelope(command: CommandDto?, elementType: String = "button") = SduiEnvelopeDto(
        protocolVersion = 1,
        schemaVersion = 1,
        screen = ScreenDto(
            id = "screen",
            version = 1,
            template = TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "form",
                        elements = listOf(ElementDto(id = "continue", type = elementType, command = command)),
                    ),
                ),
            ),
        ),
    )

    private fun registry() = SduiRegistryBuilder().apply {
        register(TestDefinition(NodeKind.TEMPLATE, "auth"))
        register(TestDefinition(NodeKind.COMPONENT, "form"))
        register(TestDefinition(NodeKind.ELEMENT, "button"))
    }.build()

    private class TestDefinition(override val kind: NodeKind, type: String) : SduiDefinition<EmptyNodeProperties> {
        override val type: NodeType = NodeType(type)
        override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<EmptyNodeProperties> =
            PropertyDecodeResult.Success(EmptyNodeProperties)
    }
}
