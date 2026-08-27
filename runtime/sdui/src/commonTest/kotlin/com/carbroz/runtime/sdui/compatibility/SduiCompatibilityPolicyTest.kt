package com.carbroz.runtime.sdui.compatibility

import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.model.EmptyNodeProperties
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.protocol.ComponentDto
import com.carbroz.runtime.sdui.protocol.ElementDto
import com.carbroz.runtime.sdui.protocol.RequestCommandDto
import com.carbroz.runtime.sdui.protocol.ScreenDto
import com.carbroz.runtime.sdui.protocol.SequenceCommandDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.TemplateDto
import com.carbroz.runtime.sdui.registry.SduiRegistryBuilder
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SduiCompatibilityPolicyTest {
    @Test
    fun compatibleEnvelopePassesAllChecks() {
        assertEquals(SduiCompatibilityResult.Compatible, policy().evaluate(envelope()))
    }

    @Test
    fun rejectsUnsupportedProtocolSchemaAndOldClient() {
        val result = assertIs<SduiCompatibilityResult.Incompatible>(
            policy(clientVersion = 3).evaluate(
                envelope(protocolVersion = 4, schemaVersion = 5, minimumClientVersion = 4),
            ),
        )
        assertEquals(
            listOf(
                SduiCompatibilityIssue.ClientTooOld(4, 3),
                SduiCompatibilityIssue.UnsupportedProtocolVersion(4),
                SduiCompatibilityIssue.UnsupportedSchemaVersion(5),
            ),
            result.issues,
        )
    }

    @Test
    fun rejectsMissingRequiredDefinitionAndCapability() {
        val result = assertIs<SduiCompatibilityResult.Incompatible>(
            policy().evaluate(
                envelope(
                    requiredDefinitions = setOf("UNKNOWN_ELEMENT"),
                    requiredCapabilities = setOf("CAMERA"),
                ),
            ),
        )
        assertEquals(
            listOf(
                SduiCompatibilityIssue.UnsupportedRequiredDefinition("UNKNOWN_ELEMENT"),
                SduiCompatibilityIssue.UnsupportedRequiredCapability("CAMERA"),
            ),
            result.issues,
        )
    }

    @Test
    fun rejectsRequestDestinationTemplateBeforeExecutionWhenNotRegistered() {
        val result = assertIs<SduiCompatibilityResult.Incompatible>(
            policy(registerDestinationTemplate = false).evaluate(envelope()),
        )
        assertEquals(
            listOf(SduiCompatibilityIssue.UnsupportedRequestDestinationTemplate("FORM_TEMPLATE")),
            result.issues,
        )
    }

    @Test
    fun noScreenRequestHasNoDestinationTemplateCompatibilityRequirement() {
        val result = policy(registerDestinationTemplate = false).evaluate(
            envelope(
                command = RequestCommandDto(
                    method = "POST",
                    endpoint = "/state",
                    responseMode = "NONE",
                    screenId = null,
                    templateId = null,
                    templateType = null,
                ),
            ),
        )
        assertEquals(SduiCompatibilityResult.Compatible, result)
    }

    @Test
    fun requestInsideSequenceIsStillCompatibilityChecked() {
        val result = assertIs<SduiCompatibilityResult.Incompatible>(
            policy(registerDestinationTemplate = false).evaluate(
                envelope(
                    command = SequenceCommandDto(
                        listOf(
                            RequestCommandDto(
                                method = "POST",
                                endpoint = "/next",
                                screenId = "next",
                                templateId = "next-template",
                                templateType = "FORM_TEMPLATE",
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(
            listOf(SduiCompatibilityIssue.UnsupportedRequestDestinationTemplate("FORM_TEMPLATE")),
            result.issues,
        )
    }

    private fun envelope(
        protocolVersion: Int = 1,
        schemaVersion: Int = 1,
        minimumClientVersion: Int = 1,
        requiredDefinitions: Set<String> = emptySet(),
        requiredCapabilities: Set<String> = emptySet(),
        command: com.carbroz.runtime.sdui.protocol.CommandDto = RequestCommandDto(
            method = "POST",
            endpoint = "/auth/send-otp",
            screenId = "otp",
            templateId = "auth_otp",
            templateType = "FORM_TEMPLATE",
        ),
    ) = SduiEnvelopeDto(
        protocolVersion = protocolVersion,
        schemaVersion = schemaVersion,
        minimumClientVersion = minimumClientVersion,
        requiredDefinitions = requiredDefinitions,
        requiredCapabilities = requiredCapabilities,
        screen = ScreenDto(
            id = "login",
            version = 1,
            template = TemplateDto(
                id = "auth_login",
                type = "AUTH_TEMPLATE",
                components = listOf(
                    ComponentDto(
                        id = "form",
                        type = "FORM_COMPONENT",
                        elements = listOf(ElementDto(id = "continue", type = "BUTTON", command = command)),
                    ),
                ),
            ),
        ),
    )

    private fun policy(
        clientVersion: Int = 10,
        registerDestinationTemplate: Boolean = true,
    ): SduiCompatibilityPolicy {
        val registry = SduiRegistryBuilder().apply {
            register(TestDefinition(NodeKind.TEMPLATE, "AUTH_TEMPLATE"))
            if (registerDestinationTemplate) register(TestDefinition(NodeKind.TEMPLATE, "FORM_TEMPLATE"))
            register(TestDefinition(NodeKind.COMPONENT, "FORM_COMPONENT"))
            register(TestDefinition(NodeKind.ELEMENT, "BUTTON"))
        }.build()
        return SduiCompatibilityPolicy(
            client = SduiClientCompatibility(
                clientVersion = clientVersion,
                supportedProtocolVersions = 1..2,
                supportedSchemaVersions = 1..3,
                supportedCapabilities = setOf("LOCATION"),
            ),
            registry = registry,
        )
    }

    private class TestDefinition(
        override val kind: NodeKind,
        type: String,
    ) : SduiDefinition<EmptyNodeProperties> {
        override val type: NodeType = NodeType(type)
        override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<EmptyNodeProperties> =
            PropertyDecodeResult.Success(EmptyNodeProperties)
    }
}
