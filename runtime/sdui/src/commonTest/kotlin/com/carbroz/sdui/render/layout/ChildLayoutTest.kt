package com.carbroz.sdui.render.layout

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChildLayoutTest {
    @Test
    fun emptyPropertiesResolveToCurrentLinearDefaults() {
        val properties = JsonObject(emptyMap())
        val spec = resolveLinearChildLayout(properties)

        assertNull(linearChildLayoutSupportError(properties))
        assertEquals(LinearAxis.VERTICAL, spec.axis)
        assertEquals(16f, spec.spacing)
        assertEquals(LinearMainAxisAlignment.START, spec.mainAxisAlignment)
        assertEquals(LinearCrossAxisAlignment.START, spec.crossAxisAlignment)
    }

    @Test
    fun spacingAndMainAxisAlignmentAreResolvedIndependently() {
        val properties = JsonObject(
            mapOf(
                "axis" to JsonPrimitive("horizontal"),
                "spacing" to JsonPrimitive(12),
                "mainAxisAlignment" to JsonPrimitive("center"),
                "crossAxisAlignment" to JsonPrimitive("end"),
            ),
        )
        val spec = resolveLinearChildLayout(properties)

        assertNull(linearChildLayoutSupportError(properties))
        assertEquals(LinearAxis.HORIZONTAL, spec.axis)
        assertEquals(12f, spec.spacing)
        assertEquals(LinearMainAxisAlignment.CENTER, spec.mainAxisAlignment)
        assertEquals(LinearCrossAxisAlignment.END, spec.crossAxisAlignment)
    }

    @Test
    fun distributedMainAxisAlignmentIsNotLostWhenSpacingIsPresent() {
        val properties = JsonObject(
            mapOf(
                "spacing" to JsonPrimitive(8),
                "mainAxisAlignment" to JsonPrimitive("spaceBetween"),
            ),
        )
        val spec = resolveLinearChildLayout(properties)

        assertNull(linearChildLayoutSupportError(properties))
        assertEquals(8f, spec.spacing)
        assertEquals(LinearMainAxisAlignment.SPACE_BETWEEN, spec.mainAxisAlignment)
    }

    @Test
    fun explicitUnknownLayoutVocabularyFailsClosed() {
        assertEquals(
            "unsupported_layout_axis:diagonal",
            linearChildLayoutSupportError(JsonObject(mapOf("axis" to JsonPrimitive("diagonal")))),
        )
        assertEquals(
            "unsupported_main_axis_alignment:stretch",
            linearChildLayoutSupportError(JsonObject(mapOf("mainAxisAlignment" to JsonPrimitive("stretch")))),
        )
        assertEquals(
            "unsupported_cross_axis_alignment:spaceBetween",
            linearChildLayoutSupportError(JsonObject(mapOf("crossAxisAlignment" to JsonPrimitive("spaceBetween")))),
        )
        assertEquals(
            "unsupported_layout_spacing:-1.0",
            linearChildLayoutSupportError(JsonObject(mapOf("spacing" to JsonPrimitive(-1)))),
        )
    }
}
