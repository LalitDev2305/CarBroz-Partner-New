package com.carbroz.runtime.sdui

import com.carbroz.runtime.sdui.compatibility.SduiClientCompatibility
import com.carbroz.runtime.sdui.compatibility.SduiCompatibilityPolicy
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.model.EmptyNodeProperties
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
import com.carbroz.runtime.sdui.protocol.SduiDecodeError
import com.carbroz.runtime.sdui.protocol.SduiDecoder
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.registry.SduiRegistry
import com.carbroz.runtime.sdui.registry.SduiRegistryBuilder
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SduiPipelineTest {
    @Test
    fun validPayloadPassesEveryMandatoryStage() {
        val result = pipeline(registry()).process(validPayload())

        val success = assertIs<SduiPipelineResult.Success>(result)
        assertEquals("screen", success.screen.id.value)
        assertEquals("screen/template", success.screen.template.path.toString())
    }

    @Test
    fun malformedPayloadStopsAtDecode() {
        val result = pipeline(registry()).process("{")

        val failure = assertIs<SduiPipelineResult.DecodeFailure>(result)
        assertEquals(SduiDecodeError.MalformedPayload, failure.error)
    }

    @Test
    fun structurallyInvalidPayloadStopsAtValidation() {
        val result = pipeline(registry()).process(validPayload(emptyComponents = true))

        assertIs<SduiPipelineResult.ValidationFailure>(result)
    }

    @Test
    fun unsupportedRequiredDefinitionStopsAtCompatibility() {
        val payload = validPayload(requiredDefinitions = listOf("MISSING"))

        val result = pipeline(registry()).process(payload)

        assertIs<SduiPipelineResult.CompatibilityFailure>(result)
    }

    @Test
    fun propertyDecodeFailureStopsAtNormalization() {
        val registry = registry(
            elementDefinition = TestDefinition(
                kind = NodeKind.ELEMENT,
                type = "BUTTON",
                decode = { PropertyDecodeResult.Failure("invalid button properties") },
            ),
        )

        val result = pipeline(registry).process(validPayload())

        assertIs<SduiPipelineResult.NormalizationFailure>(result)
    }

    @Test
    fun unsupportedRequestDestinationTemplateStopsAtCompatibilityBeforeNormalization() {
        val result = pipeline(registry()).process(
            validPayload(commandTemplateType = "GRID_TEMPLATE"),
        )

        assertIs<SduiPipelineResult.CompatibilityFailure>(result)
    }

    private fun pipeline(registry: SduiRegistry) = SduiPipeline(
        decoder = SduiDecoder(),
        validator = SduiSchemaValidator(),
        compatibilityPolicy = SduiCompatibilityPolicy(
            client = SduiClientCompatibility(
                clientVersion = 1,
                supportedProtocolVersions = 1..1,
                supportedSchemaVersions = 1..1,
            ),
            registry = registry,
        ),
        normalizer = SduiNormalizer(registry),
    )

    private fun registry(
        elementDefinition: SduiDefinition<out NodeProperties> = TestDefinition(NodeKind.ELEMENT, "BUTTON"),
    ): SduiRegistry = SduiRegistryBuilder().apply {
        register(TestDefinition(NodeKind.TEMPLATE, "FORM_TEMPLATE"))
        register(TestDefinition(NodeKind.COMPONENT, "FORM"))
        register(elementDefinition)
    }.build()

    private fun validPayload(
        commandTemplateType: String = "FORM_TEMPLATE",
        requiredDefinitions: List<String> = emptyList(),
        emptyComponents: Boolean = false,
    ): String {
        val required = requiredDefinitions.joinToString(",") { "\"$it\"" }
        val components = if (emptyComponents) {
            "[]"
        } else {
            """[{
                "id":"form",
                "type":"FORM",
                "elements":[{
                  "id":"continue",
                  "type":"BUTTON",
                  "command":{
                    "kind":"REQUEST",
                    "method":"POST",
                    "endpoint":"/auth/send-otp",
                    "screenId":"otp",
                    "templateId":"auth_otp",
                    "templateType":"$commandTemplateType",
                    "payload":{}
                  }
                }]
              }]"""
        }

        return """
            {
              "protocolVersion":1,
              "schemaVersion":1,
              "minimumClientVersion":1,
              "requiredDefinitions":[$required],
              "requiredCapabilities":[],
              "screen":{
                "id":"screen",
                "version":1,
                "template":{
                  "id":"template",
                  "type":"FORM_TEMPLATE",
                  "components":$components
                }
              }
            }
        """.trimIndent()
    }

    private class TestDefinition(
        override val kind: NodeKind,
        type: String,
        private val decode: (JsonObject) -> PropertyDecodeResult<NodeProperties> = {
            PropertyDecodeResult.Success(EmptyNodeProperties)
        },
    ) : SduiDefinition<NodeProperties> {
        override val type: NodeType = NodeType(type)

        override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<NodeProperties> = decode(raw)
    }
}
