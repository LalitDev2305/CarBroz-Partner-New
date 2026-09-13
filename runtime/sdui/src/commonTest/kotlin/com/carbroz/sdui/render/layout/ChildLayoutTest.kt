package com.carbroz.sdui.render.layout

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ChildLayoutTest {
    @Test
    fun emptyPropertiesResolveToCurrentLinearDefaults() {
        val spec = resolveLinearChildLayout(JsonObject(emptyMap()))

        assertEquals(LinearAxis.VERTICAL, spec.axis)
        assertEquals(16f, spec.spacing)
        assertEquals(LinearMainAxisAlignment.START, spec.mainAxisAlignment)
        assertEquals(LinearCrossAxisAlignment.START, spec.crossAxisAlignment)
    }

    @Test
    fun spacingAndMainAxisAlignmentAreResolvedIndependently() {
        val spec = resolveLinearChildLayout(
            JsonObject(
                mapOf(
                    "axis" to JsonPrimitive("horizontal"),
                    "spacing" to JsonPrimitive(12),
                    "mainAxisAlignment" to JsonPrimitive("center"),
                    "crossAxisAlignment" to JsonPrimitive("end"),
                ),
            ),
        )

        assertEquals(LinearAxis.HORIZONTAL, spec.axis)
        assertEquals(12f, spec.spacing)
        assertEquals(LinearMainAxisAlignment.CENTER, spec.mainAxisAlignment)
        assertEquals(LinearCrossAxisAlignment.END, spec.crossAxisAlignment)
    }

    @Test
    fun distributedMainAxisAlignmentIsNotLostWhenSpacingIsPresent() {
        val spec = resolveLinearChildLayout(
            JsonObject(
                mapOf(
                    "spacing" to JsonPrimitive(8),
                    "mainAxisAlignment" to JsonPrimitive("spaceBetween"),
                ),
            ),
        )

        assertEquals(8f, spec.spacing)
        assertEquals(LinearMainAxisAlignment.SPACE_BETWEEN, spec.mainAxisAlignment)
    }
}
