package com.carbroz.partner.sdui.engine.processor

import com.carbroz.partner.sdui.engine.result.SduiParseError
import com.carbroz.partner.sdui.engine.result.SduiParseResult
import com.carbroz.partner.sdui.engine.validation.SduiValidationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SduiProcessorTest {

    private val validJson = """
    {
      "schema_version": 1,
      "screen_id": "screen_test",
      "title": "Test Screen",
      "show_back": true,
      "template": {
        "template_id": "tpl_1",
        "template_type": "form_template",
        "width": "fill",
        "height": "fill",
        "axis": "vertical",
        "components": [
          {
            "component_id": "comp_1",
            "component_type": "card",
            "width": "fill",
            "height": "wrap",
            "axis": "vertical",
            "subcomponents": [],
            "children_data": []
          }
        ]
      }
    }
    """.trimIndent()

    private val invalidVersionJson = """
    {
      "schema_version": 99,
      "screen_id": "screen_test",
      "title": "Test Screen",
      "show_back": true,
      "template": {
        "template_id": "tpl_1",
        "template_type": "form_template",
        "width": "fill",
        "height": "fill",
        "axis": "vertical",
        "components": []
      }
    }
    """.trimIndent()

    @Test
    fun testProcessValidJsonReturnsAssembledScreen() {
        val processor = SduiProcessor()
        val result = processor.process(validJson, SduiValidationPolicy())
        assertTrue(result is SduiParseResult.Success)
        val assembled = (result as SduiParseResult.Success).assembledScreen
        assertEquals("screen_test", assembled.screen.screenId)
        assertTrue(assembled.nodeIndex["comp_1"] != null)
    }

    @Test
    fun testProcessMalformedJsonReturnsParseFailure() {
        val processor = SduiProcessor()
        val result = processor.process("{ invalid_json }", SduiValidationPolicy())
        assertTrue(result is SduiParseResult.Failure)
        assertTrue((result as SduiParseResult.Failure).error is SduiParseError.MalformedJson)
    }

    @Test
    fun testProcessValidationFailureReturnsTypedError() {
        val processor = SduiProcessor()
        val result = processor.process(invalidVersionJson, SduiValidationPolicy())
        assertTrue(result is SduiParseResult.Failure)
        assertTrue((result as SduiParseResult.Failure).error is SduiParseError.UnsupportedSchemaVersion)
    }
}
