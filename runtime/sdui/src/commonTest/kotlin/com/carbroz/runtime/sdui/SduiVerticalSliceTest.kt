package com.carbroz.runtime.sdui

import com.carbroz.runtime.sdui.compatibility.SduiClientCompatibility
import com.carbroz.runtime.sdui.compatibility.SduiCompatibilityPolicy
import com.carbroz.runtime.sdui.interaction.SduiCommandIndex
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
import com.carbroz.runtime.sdui.protocol.SduiDecoder
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.registry.CoreSduiDefinitions
import com.carbroz.runtime.sdui.registry.SduiRegistryBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SduiVerticalSliceTest {
    @Test
    fun corePayloadNormalizesFullHierarchyAndIndexesButtonCommand() {
        val registry = SduiRegistryBuilder().apply {
            registerAll(CoreSduiDefinitions.all)
        }.build()
        val pipeline = SduiPipeline(
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

        val result = pipeline.process(payload)
        val screen = assertIs<SduiPipelineResult.Success>(result).screen

        val component = screen.template.components.single()
        val commandIndex = SduiCommandIndex.from(screen)
        assertEquals("login/form/credentials/fields/continue", commandIndexPath(screen))
        assertEquals(1, commandIndex.size)

        val command = assertNotNull(commandIndex.commandAt(buttonPath(screen)))
        val request = assertIs<RequestCommand>(command)
        assertEquals("/auth/send-otp", request.endpoint)
        assertEquals("otp", request.destination.screenId)
        assertEquals("auth_otp", request.destination.templateId)
        assertEquals("FORM_TEMPLATE", request.destination.templateType.value)
        assertEquals("STACK", component.type.value)
    }

    private fun buttonPath(screen: com.carbroz.runtime.sdui.model.Screen) =
        screen.template.components.single()
            .let { component ->
                val sections = assertIs<com.carbroz.runtime.sdui.model.ComponentContent.Sections>(component.content)
                val section = sections.values.single()
                val groups = assertIs<com.carbroz.runtime.sdui.model.SectionContent.Groups>(section.content)
                groups.values.single().elements.last().path
            }

    private fun commandIndexPath(screen: com.carbroz.runtime.sdui.model.Screen): String =
        buttonPath(screen).toString()

    private val payload = """
        {
          "protocolVersion": 1,
          "schemaVersion": 1,
          "minimumClientVersion": 1,
          "screen": {
            "id": "login",
            "version": 1,
            "template": {
              "id": "form",
              "type": "FORM_TEMPLATE",
              "components": [
                {
                  "id": "credentials",
                  "type": "STACK",
                  "sections": [
                    {
                      "id": "fields",
                      "type": "STACK",
                      "groups": [
                        {
                          "id": "fields",
                          "type": "STACK",
                          "elements": [
                            {
                              "id": "title",
                              "type": "TEXT",
                              "properties": {
                                "text": "Welcome Back",
                                "style": "TITLE_LARGE"
                              }
                            },
                            {
                              "id": "continue",
                              "type": "BUTTON",
                              "properties": {
                                "text": "Continue"
                              },
                              "command": {
                                "kind": "REQUEST",
                                "method": "POST",
                                "endpoint": "/auth/send-otp",
                                "screenId": "otp",
                                "templateId": "auth_otp",
                                "templateType": "FORM_TEMPLATE",
                                "payload": {
                                  "phone": "${'$'}form.phone"
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          }
        }
    """.trimIndent()
}
