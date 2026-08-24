package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SduiProtocolTest {
    @Test
    fun decoderAcceptsStrictValidPayload() {
        val payload = """
            {
              "protocolVersion": 1,
              "schemaVersion": 1,
              "screen": {
                "id": "screen-1",
                "version": 1,
                "template": {
                  "id": "template-1",
                  "type": "auth",
                  "components": [{
                    "id": "component-1",
                    "type": "form",
                    "elements": [{
                      "id": "title",
                      "type": "text",
                      "properties": {"text":"Hello","typography":"TITLE_LARGE"}
                    }]
                  }]
                }
              }
            }
        """.trimIndent()

        assertIs<SduiDecodeResult.Success>(SduiDecoder().decode(payload))
    }

    @Test
    fun decoderRejectsUnknownTransportFields() {
        val payload = """
            {
              "protocolVersion": 1,
              "schemaVersion": 1,
              "unexpected": true,
              "screen": {
                "id": "screen-1",
                "version": 1,
                "template": {"id":"template-1","type":"auth","components":[]}
              }
            }
        """.trimIndent()

        assertEquals(
            SduiDecodeResult.Failure(SduiDecodeError.MalformedPayload),
            SduiDecoder().decode(payload),
        )
    }

    @Test
    fun decoderRejectsOversizedPayloadBeforeParsing() {
        val decoder = SduiDecoder(SduiProtocolLimits(maxPayloadCharacters = 4))
        assertEquals(
            SduiDecodeResult.Failure(SduiDecodeError.PayloadTooLarge),
            decoder.decode("12345"),
        )
    }

    @Test
    fun validatorAcceptsFullSemanticHierarchy() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "form",
                        sections = listOf(
                            SectionDto(
                                id = "credentials",
                                type = "credentials",
                                groups = listOf(
                                    GroupDto(
                                        id = "fields",
                                        type = "field-group",
                                        elements = listOf(element("email", "input"), element("password", "input")),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
    }

    @Test
    fun validatorAcceptsComponentTerminatingDirectlyInElements() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "header",
                        elements = listOf(element("title", "text")),
                    ),
                ),
            ),
        )

        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
    }

    @Test
    fun validatorRejectsConflictingComponentBranches() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "form",
                        sections = listOf(
                            SectionDto(id = "section", type = "credentials", elements = listOf(element("email", "input"))),
                        ),
                        elements = listOf(element("title", "text")),
                    ),
                ),
            ),
        )

        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.ConflictingBranchContent, invalid.violations.single().code)
    }

    @Test
    fun validatorRejectsDuplicateSiblingIdentifiersButAllowsSameIdInDifferentBranches() {
        val duplicateSibling = envelope(
            TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "form",
                        elements = listOf(element("label", "text"), element("label", "text")),
                    ),
                ),
            ),
        )
        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(duplicateSibling))
        assertEquals(SduiViolationCode.DuplicateSiblingIdentifier, invalid.violations.single().code)

        val reusableAcrossBranches = envelope(
            TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(id = "one", type = "header", elements = listOf(element("label", "text"))),
                    ComponentDto(id = "two", type = "footer", elements = listOf(element("label", "text"))),
                ),
            ),
        )
        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(reusableAcrossBranches))
    }

    @Test
    fun validatorRejectsGroupWithoutTerminalElements() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "auth",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "form",
                        sections = listOf(
                            SectionDto(
                                id = "section",
                                type = "credentials",
                                groups = listOf(GroupDto(id = "group", type = "fields", elements = emptyList())),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.MissingTerminalElements, invalid.violations.single().code)
    }

    private fun envelope(template: TemplateDto) = SduiEnvelopeDto(
        protocolVersion = 1,
        schemaVersion = 1,
        screen = ScreenDto(id = "screen", version = 1, template = template),
    )

    private fun element(id: String, type: String) = ElementDto(
        id = id,
        type = type,
        properties = JsonObject(mapOf("value" to JsonPrimitive("value"))),
    )
}
