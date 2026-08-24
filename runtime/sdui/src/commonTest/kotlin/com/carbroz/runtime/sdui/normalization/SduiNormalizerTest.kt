package com.carbroz.runtime.sdui.normalization

import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.model.EmptyNodeProperties
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
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

class SduiNormalizerTest {
    @Test
    fun validRequestCommandNormalizesIntoTrustedDestinationContract() {
        val binding = "\$form.phone"
        val envelope = envelope(
            RequestCommandDto(
                method = "post",
                endpoint = "/auth/send-otp",
                screenId = "otp",
                templateId = "auth_otp",
                templateType = "FORM_TEMPLATE",
                payload = JsonObject(mapOf("phone" to JsonPrimitive(binding))),
            ),
        )
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))

        val result = assertIs<SduiNormalizationResult.Success>(
            SduiNormalizer(registry()).normalize(envelope),
        )
        val command = assertIs<RequestCommand>(
            (result.screen.template.components.single().content as com.carbroz.runtime.sdui.model.ComponentContent.Elements)
                .values.single().command,
        )

        assertEquals(RequestMethod.POST, command.method)
        assertEquals("/auth/send-otp", command.endpoint)
        assertEquals("otp", command.destination.screenId)
        assertEquals("auth_otp", command.destination.templateId)
        assertEquals(NodeType("FORM_TEMPLATE"), command.destination.templateType)
        assertEquals(JsonPrimitive(binding), command.payload["phone"])
    }

    @Test
    fun validatorRejectsAbsoluteRequestEndpoint() {
        val envelope = envelope(
            RequestCommandDto(
                method = "POST",
                endpoint = "https://evil.example/send-otp",
                screenId = "otp",
                templateId = "auth_otp",
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
                endpoint = "/auth/send-otp",
                screenId = "otp",
                templateId = "auth_otp",
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

        val result = assertIs<SduiNormalizationResult.Failure>(
            SduiNormalizer(registry()).normalize(envelope),
        )
        assertIs<SduiNormalizationError.UnsupportedDefinition>(result.error)
    }

    @Test
    fun normalizedNodePathUsesSemanticIdsRatherThanIndexes() {
        val result = assertIs<SduiNormalizationResult.Success>(
            SduiNormalizer(registry()).normalize(envelope(command = null)),
        )
        val element = (result.screen.template.components.single().content as com.carbroz.runtime.sdui.model.ComponentContent.Elements)
            .values.single()

        assertEquals("screen/template/component/continue", element.path.toString())
    }

    private fun envelope(
        command: RequestCommandDto?,
        elementType: String = "button",
    ) = SduiEnvelopeDto(
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
                        elements = listOf(
                            ElementDto(
                                id = "continue",
                                type = elementType,
                                command = command,
                            ),
                        ),
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

    private class TestDefinition(
        override val kind: NodeKind,
        type: String,
    ) : SduiDefinition<EmptyNodeProperties> {
        override val type: NodeType = NodeType(type)

        override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<EmptyNodeProperties> =
            PropertyDecodeResult.Success(EmptyNodeProperties)
    }
}
