package com.carbroz.foundation.designsystem

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InteractionSizingTest {
    @Test
    fun defaultMinimumTargetIs48Dp() {
        assertEquals(48.dp, InteractionSizing.Default.minimumTarget)
    }

    @Test
    fun customPositiveTargetIsAccepted() {
        assertEquals(56.dp, InteractionSizing(56.dp).minimumTarget)
    }

    @Test
    fun zeroTargetIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            InteractionSizing(0.dp)
        }
    }

    @Test
    fun negativeTargetIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            InteractionSizing((-1).dp)
        }
    }
}
