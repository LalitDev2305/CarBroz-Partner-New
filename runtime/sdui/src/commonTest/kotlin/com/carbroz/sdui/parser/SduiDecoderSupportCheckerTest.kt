package com.carbroz.sdui.parser

import com.carbroz.sdui.model.RequestPayload
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiTargetApp
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.model.SduiTheme
import com.carbroz.sdui.registry.SduiNodeRegistration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SduiDecoderSupportCheckerTest {
    private val decoder = SduiDecoder()
    private val checker = SduiSupportChecker(SduiNodeRegistration.createRegistry())

    @Test
    fun canonicalScreenJson_decodesDirectlyFromJsonElement() {
        val element = Json.parseToJsonElement(
            """
            {
              "screenId": "auth_login",
              "schemaVersion": "3.0",
              "targetApp": "CUSTOMER",
              "template": {
                "id": "login",
                "type": "form_template",
                "properties": {},
                "components": [
                  {
                    "id": "root",
                    "type": "stack_component",
                    "properties": {},
                    "elements": [
                      {"id": "title", "type": "text", "properties": {"text": "Login"}}
                    ]
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        val result = assertIs<SduiDecodeResult.Success>(decoder.decode(element))

        assertEquals("auth_login", result.screen.screenId)
        assertEquals("form_template", result.screen.template.type)
        assertEquals("stack_component", result.screen.template.components.single().type)
        assertEquals("text", result.screen.template.components.single().elements.orEmpty().single().type)
    }

    @Test
    fun malformedRequiredData_failsPredictably() {
        val result = decoder.decode(Json.parseToJsonElement("{\"screenId\":\"missing-contract\"}"))

        assertIs<SduiDecodeResult.Failure>(result)
    }

    @Test
    fun unknownActionType_failsInsteadOfBeingGuessed() {
        val element = Json.parseToJsonElement(
            """
            {
              "screenId": "unknown_action",
              "schemaVersion": "3.0",
              "targetApp": "CUSTOMER",
              "template": {
                "id": "screen",
                "type": "form_template",
                "components": [
                  {
                    "id": "root",
                    "type": "stack_component",
                    "elements": [
                      {
                        "id": "button",
                        "type": "button",
                        "actions": {"click": {"type": "invented_action"}}
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        assertIs<SduiDecodeResult.Failure>(decoder.decode(element))
    }

    @Test
    fun registeredContract_isSupported() {
        assertIs<SduiSupportResult.Supported>(checker.check(screen()))
    }

    @Test
    fun globalTarget_isSupported() {
        assertIs<SduiSupportResult.Supported>(checker.check(screen().copy(targetApp = SduiTargetApp.GLOBAL)))
    }

    @Test
    fun customerTarget_isRejectedByPartnerRuntime() {
        val result = checker.check(screen().copy(targetApp = SduiTargetApp.CUSTOMER))

        assertEquals(SduiSupportResult.Unsupported("unsupported_target_app:CUSTOMER"), result)
    }

    @Test
    fun decodedButUnconsumedTheme_isRejected() {
        val result = checker.check(screen().copy(theme = SduiTheme()))

        assertEquals(SduiSupportResult.Unsupported("unsupported_theme"), result)
    }

    @Test
    fun unsupportedSchema_isRejectedAsCapabilityMismatch() {
        val result = checker.check(screen().copy(schemaVersion = "99.0"))

        assertEquals(SduiSupportResult.Unsupported("unsupported_schema:99.0"), result)
    }

    @Test
    fun unsupportedTemplate_isRejected() {
        val result = checker.check(screen().copy(template = screen().template.copy(type = "unknown_template")))

        assertEquals(SduiSupportResult.Unsupported("unsupported_template:unknown_template"), result)
    }

    @Test
    fun unsupportedElement_isRejected() {
        val base = screen()
        val component = base.template.components.single().copy(
            elements = listOf(base.template.components.single().elements.orEmpty().single().copy(type = "unknown_element")),
        )
        val result = checker.check(base.copy(template = base.template.copy(components = listOf(component))))

        assertEquals(SduiSupportResult.Unsupported("unsupported_element:unknown_element"), result)
    }

    @Test
    fun explicitUnknownLayoutVocabulary_isRejectedInsteadOfFallingBack() {
        val base = screen()
        val result = checker.check(
            base.copy(
                template = base.template.copy(
                    properties = JsonObject(mapOf("axis" to JsonPrimitive("diagonal"))),
                ),
            ),
        )

        assertEquals(SduiSupportResult.Unsupported("unsupported_layout_axis:diagonal"), result)
    }

    @Test
    fun unsupportedAccessoryType_isRejectedInsteadOfDisappearing() {
        val base = screen()
        val element = base.template.components.single().elements.orEmpty().single().copy(
            properties = JsonObject(
                mapOf(
                    "leading" to JsonObject(mapOf("type" to JsonPrimitive("future_accessory"))),
                ),
            ),
        )
        val component = base.template.components.single().copy(elements = listOf(element))
        val result = checker.check(base.copy(template = base.template.copy(components = listOf(component))))

        assertEquals(SduiSupportResult.Unsupported("unsupported_accessory:future_accessory"), result)
    }

    @Test
    fun unsupportedInputWeight_isRejectedInsteadOfBeingMisreadAsFillWidth() {
        val base = screen()
        val input = SduiElement(
            id = "input",
            type = "input",
            properties = JsonObject(mapOf("weight" to JsonPrimitive(1))),
        )
        val component = base.template.components.single().copy(elements = listOf(input))
        val result = checker.check(base.copy(template = base.template.copy(components = listOf(component))))

        assertEquals(SduiSupportResult.Unsupported("unsupported_input_weight"), result)
    }

    @Test
    fun absoluteRequestEndpoint_isRejectedAtSecurityBoundary() {
        val base = screen()
        val request = SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = "https://example.com/auth",
                authentication = SduiAuthentication.NONE,
            ),
        )
        val element = base.template.components.single().elements.orEmpty().single().copy(
            actions = mapOf("click" to request),
        )
        val component = base.template.components.single().copy(elements = listOf(element))
        val result = checker.check(base.copy(template = base.template.copy(components = listOf(component))))

        assertEquals(SduiSupportResult.Unsupported("unsafe_endpoint"), result)
    }

    private fun screen(): SduiScreen = SduiScreen(
        screenId = "test_screen",
        schemaVersion = "3.0",
        targetApp = SduiTargetApp.PARTNER,
        template = SduiTemplate(
            id = "test_template",
            type = "form_template",
            properties = JsonObject(emptyMap()),
            components = listOf(
                SduiComponent(
                    id = "root",
                    type = "stack_component",
                    properties = JsonObject(emptyMap()),
                    elements = listOf(
                        SduiElement(
                            id = "title",
                            type = "text",
                            properties = JsonObject(emptyMap()),
                        ),
                    ),
                ),
            ),
        ),
    )
}
