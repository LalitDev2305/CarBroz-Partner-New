package com.carbroz.runtime.sdui.protocol

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
                  "type": "stack",
                  "children": [{
                    "id": "child-1",
                    "type": "content",
                    "data": [{"id":"data-1","type":"text","payload":"Hello"}]
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
                "template": {"id":"template-1","type":"stack","children":[]}
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
    fun validatorAcceptsVariableDepthWhenEveryBranchTerminatesInChildData() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "stack",
                components = listOf(
                    ComponentDto(
                        id = "component",
                        type = "section",
                        subComponents = listOf(
                            SubComponentDto(
                                id = "sub",
                                type = "group",
                                children = listOf(child("nested-child", "nested-data")),
                            ),
                        ),
                    ),
                ),
                children = listOf(child("direct-child", "direct-data")),
            ),
        )

        assertEquals(SduiValidationResult.Valid, SduiSchemaValidator().validate(envelope))
    }

    @Test
    fun validatorRejectsEmptyIntermediateBranch() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "stack",
                components = listOf(ComponentDto(id = "component", type = "section")),
            ),
        )

        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.EmptyBranch, invalid.violations.single().code)
    }

    @Test
    fun validatorRejectsDuplicateIdentifiersAcrossHierarchy() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "stack",
                children = listOf(child("screen", "data")),
            ),
        )

        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.DuplicateIdentifier, invalid.violations.single().code)
    }

    @Test
    fun validatorRejectsMissingTerminalData() {
        val envelope = envelope(
            TemplateDto(
                id = "template",
                type = "stack",
                children = listOf(ChildDto(id = "child", type = "content", data = emptyList())),
            ),
        )

        val invalid = assertIs<SduiValidationResult.Invalid>(SduiSchemaValidator().validate(envelope))
        assertEquals(SduiViolationCode.MissingTerminalData, invalid.violations.single().code)
    }

    private fun envelope(template: TemplateDto) = SduiEnvelopeDto(
        protocolVersion = 1,
        schemaVersion = 1,
        screen = ScreenDto(id = "screen", version = 1, template = template),
    )

    private fun child(id: String, dataId: String) = ChildDto(
        id = id,
        type = "content",
        data = listOf(ChildDataDto(dataId, "text", JsonPrimitive("value"))),
    )
}
