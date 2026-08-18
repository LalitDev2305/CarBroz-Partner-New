package com.carbroz.partner.sdui.render.renderer.childdata

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.carbroz.partner.sdui.render.renderer.childdata.button.ButtonChildrenDataProperties
import com.carbroz.partner.sdui.render.renderer.childdata.input.InputChildrenDataProperties
import com.carbroz.partner.sdui.render.renderer.childdata.text.TextChildrenDataProperties
import com.carbroz.partner.sdui.render.renderer.childdata.timer.TimerChildrenDataProperties
import kotlin.test.Test
import kotlin.test.assertEquals

class ChildrenDataPropertiesTest {

    @Test
    fun testTextChildrenDataPropertiesDecoding() {
        val json = buildJsonObject {
            put("text", "Hello World")
            put("style", "heading_lg")
            put("color", "#FF0000")
        }
        val props = TextChildrenDataProperties.decode(json)
        assertEquals("Hello World", props.text)
        assertEquals("heading_lg", props.styleKey)
        assertEquals("#FF0000", props.colorString)
    }

    @Test
    fun testButtonChildrenDataPropertiesDecoding() {
        val json = buildJsonObject {
            put("text", "Submit")
            put("style", "primary")
        }
        val props = ButtonChildrenDataProperties.decode(json)
        assertEquals("Submit", props.text)
        assertEquals("primary", props.styleKey)
    }

    @Test
    fun testInputChildrenDataPropertiesDecoding() {
        val json = buildJsonObject {
            put("placeholder", "Enter name")
            put("label", "Name")
        }
        val props = InputChildrenDataProperties.decode(json)
        assertEquals("Enter name", props.placeholder)
        assertEquals("Name", props.label)
    }

    @Test
    fun testTimerChildrenDataPropertiesDecoding() {
        val json = buildJsonObject {
            put("initial_seconds", 120)
            put("format", "mm:ss")
        }
        val props = TimerChildrenDataProperties.decode(json)
        assertEquals(120, props.initialSeconds)
        assertEquals("mm:ss", props.format)
    }

    @Test
    fun testNullJsonObjectSafeDefaults() {
        assertEquals("", TextChildrenDataProperties.decode(null).text)
        assertEquals("", ButtonChildrenDataProperties.decode(null).text)
        assertEquals("", InputChildrenDataProperties.decode(null).placeholder)
        assertEquals(60, TimerChildrenDataProperties.decode(null).initialSeconds)
    }
}
