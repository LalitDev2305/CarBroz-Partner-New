package com.carbroz.partner.sdui.engine.parser

import com.carbroz.partner.sdui.engine.raw.RawScreenDto
import com.carbroz.partner.sdui.engine.raw.RawTemplateDto
import com.carbroz.partner.sdui.engine.raw.RawThemeDto
import com.carbroz.partner.sdui.engine.raw.RawComponentDto
import com.carbroz.partner.sdui.engine.raw.RawSubComponentDto
import com.carbroz.partner.sdui.engine.raw.RawChildDto
import com.carbroz.partner.sdui.engine.raw.RawChildrenDataDto
import com.carbroz.partner.sdui.engine.raw.RawEdgeSpacingDto
import com.carbroz.partner.sdui.engine.raw.RawActionDto
import com.carbroz.partner.sdui.engine.raw.RawParentActionDto
import com.carbroz.partner.sdui.engine.result.SduiParseError
import com.carbroz.partner.sdui.engine.result.SduiParseResult
import com.carbroz.partner.sdui.engine.assembly.DefaultSduiScreenAssembler
import com.carbroz.partner.sdui.engine.assembly.SduiScreenAssembler
import com.carbroz.partner.sdui.engine.model.SduiScreen
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiAction
import com.carbroz.partner.sdui.engine.model.SduiParentAction
import com.carbroz.partner.sdui.engine.model.SduiBindable
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.validation.SduiValidationPolicy
import com.carbroz.partner.sdui.engine.validation.SduiValidator
import com.carbroz.partner.sdui.engine.validation.DefaultSduiValidator
import com.carbroz.partner.sdui.engine.mapper.SduiMapper
import com.carbroz.partner.sdui.engine.mapper.DefaultSduiMapper
import kotlin.test.*

class SduiEngineTest {

    private val assembler: SduiScreenAssembler = DefaultSduiScreenAssembler()

    @Test
    fun testSnakeCaseDeserializationAndBasicAssembly() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_test_login",
          "title": "Partner Login",
          "show_back": false,
          "template": {
            "template_id": "tpl_login",
            "template_type": "form",
            "width": "fill",
            "height": "fill",
            "axis": "vertical",
            "components": [
              {
                "component_id": "cmp_login_body",
                "component_type": "section",
                "children_data": [
                  {
                    "children_data_id": "inp_phone",
                    "children_data_type": "input",
                    "width": "fill",
                    "height": "wrap"
                  },
                  {
                    "children_data_id": "btn_send_otp",
                    "children_data_type": "button",
                    "width": "fill",
                    "height": "wrap",
                    "action": {
                      "api": "/partner/auth/send-otp",
                      "template_id": "tpl_otp",
                      "template_type": "form"
                    }
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Success)
        val assembled = result.assembledScreen
        val screen = assembled.screen
        val nodeIndex = assembled.nodeIndex
        assertEquals(1, screen.schemaVersion)
        assertEquals("screen_test_login", screen.screenId)
        assertEquals("Partner Login", screen.title)
        assertFalse(screen.showBack)
        assertEquals("tpl_login", screen.template.id)
        assertEquals("form", screen.template.templateType)

        val cmp = screen.template.components.first()
        assertEquals("cmp_login_body", cmp.id)
        assertEquals(2, cmp.childrenData.size)

        val btn = cmp.childrenData[1]
        val act = btn.action
        assertNotNull(act)
        assertEquals("/partner/auth/send-otp", act.api)
        assertEquals("tpl_otp", act.templateId)
        assertEquals("form", act.templateType)
    }

    @Test
    fun testOptionalIntermediateHierarchyAndParentAction() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_otp",
          "show_back": true,
          "template": {
            "template_id": "tpl_otp_root",
            "template_type": "form",
            "components": [
              {
                "component_id": "cmp_timer_section",
                "component_type": "card",
                "subcomponents": [
                  {
                    "subcomponent_id": "sub_timer_row",
                    "subcomponent_type": "row",
                    "children": [
                      {
                        "child_id": "cell_timer",
                        "child_type": "cell",
                        "children_data": [
                          {
                            "children_data_id": "otp_timer",
                            "children_data_type": "timer",
                            "properties": {
                              "accepts_parent_action": true
                            }
                          },
                          {
                            "children_data_id": "btn_resend",
                            "children_data_type": "button",
                            "parent_action": {
                              "target_id": "otp_timer"
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
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Success)
        val assembled = result.assembledScreen
        val screen = assembled.screen
        val nodeIndex = assembled.nodeIndex
        assertEquals(5, nodeIndex.size)
        assertFalse(nodeIndex.containsKey("tpl_root"))
        assertTrue(nodeIndex.containsKey("otp_timer"))
        assertTrue(nodeIndex.containsKey("btn_resend"))

        val timerNode = nodeIndex["otp_timer"]
        assertNotNull(timerNode)
        assertTrue(timerNode.acceptsParentAction)

        val resendBtn = screen.template.components[0].subcomponents[0].children[0].childrenData[1]
        assertEquals("otp_timer", resendBtn.parentAction?.targetId)
    }

    @Test
    fun testComponentLevelParentActionControllerAndReceiver() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_component_parent_action",
          "template": {
            "template_id": "tpl_comp",
            "template_type": "dashboard",
            "components": [
              {
                "component_id": "cmp_details",
                "component_type": "card",
                "properties": {
                  "accepts_parent_action": true
                },
                "children_data": [
                  {
                    "children_data_id": "txt_detail",
                    "children_data_type": "text"
                  }
                ]
              },
              {
                "component_id": "cmp_controller",
                "component_type": "button_card",
                "parent_action": {
                  "target_id": "cmp_details"
                },
                "children_data": [
                  {
                    "children_data_id": "btn_toggle",
                    "children_data_type": "button"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Success)
        val assembled = result.assembledScreen
        val screen = assembled.screen
        val nodeIndex = assembled.nodeIndex
        val detailsCmp = nodeIndex["cmp_details"]
        assertNotNull(detailsCmp)
        assertTrue(detailsCmp.acceptsParentAction)

        val ctrlCmp = screen.template.components[1]
        assertEquals("cmp_details", ctrlCmp.parentAction?.targetId)
    }

    @Test
    fun testDuplicateNodeIdRejection() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_dup",
          "template": {
            "template_id": "duplicate_id",
            "template_type": "dashboard",
            "components": [
              {
                "component_id": "duplicate_id",
                "component_type": "card",
                "children_data": [
                  {
                    "children_data_id": "txt_1",
                    "children_data_type": "text"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Failure)
        assertEquals("DUPLICATE_NODE_ID", result.error.code)
    }

    @Test
    fun testParentActionTargetMissingAcceptsMarkerRejection() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_invalid_target",
          "template": {
            "template_id": "tpl_1",
            "template_type": "dashboard",
            "components": [
              {
                "component_id": "cmp_target",
                "component_type": "card",
                "children_data": [
                  {
                    "children_data_id": "txt_1",
                    "children_data_type": "text"
                  }
                ]
              },
              {
                "component_id": "cmp_source",
                "component_type": "card",
                "parent_action": {
                  "target_id": "cmp_target"
                },
                "children_data": [
                  {
                    "children_data_id": "btn_1",
                    "children_data_type": "button"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Failure)
        assertEquals("TARGET_DOES_NOT_ACCEPT_PARENT_ACTION", result.error.code)
    }

    @Test
    fun testIncompleteTemplateTransitionRejection() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_incomplete_action",
          "template": {
            "template_id": "tpl_1",
            "template_type": "form",
            "components": [
              {
                "component_id": "cmp_1",
                "component_type": "card",
                "children_data": [
                  {
                    "children_data_id": "btn_1",
                    "children_data_type": "button",
                    "action": {
                      "api": "/api/test",
                      "template_id": "tpl_next"
                    }
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Failure)
        assertEquals("INCOMPLETE_TEMPLATE_TRANSITION", result.error.code)
    }

    @Test
    fun testPaddingAndMarginNormalization() {
        val json = """
        {
          "schema_version": 1,
          "screen_id": "screen_padding",
          "template": {
            "template_id": "tpl_1",
            "template_type": "form",
            "padding": { "top": 12, "bottom": 16, "start": 8, "end": 8 },
            "margin": { "top": 4, "bottom": 4, "start": 0, "end": 0 },
            "components": [
              {
                "component_id": "cmp_1",
                "component_type": "card",
                "children_data": [
                  {
                    "children_data_id": "txt_1",
                    "children_data_type": "text"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()

        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Success)
        val t = result.assembledScreen.screen.template
        assertEquals(12, t.padding.top)
        assertEquals(16, t.padding.bottom)
        assertEquals(8, t.padding.start)
        assertEquals(8, t.padding.end)
        assertEquals(4, t.margin.top)
    }

    @Test
    fun testMalformedJsonReturnsFailure() {
        val json = "{ malformed json }"
        val result = assembler.assemble(json)
        assertTrue(result is SduiParseResult.Failure)
        assertEquals("MALFORMED_JSON", result.error.code)
    }
}
