package com.carbroz.foundation.adaptive

import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveContextTest {
    @Test
    fun defaultContextRepresentsOrdinaryTouchEnvironment() {
        val context = AdaptiveContext()

        assertEquals(AdaptivePosture.Normal, context.posture)
        assertEquals(AdaptiveInputMode.Touch, context.inputMode)
        assertTrue(context.occlusions.isEmpty())
    }

    @Test
    fun contextCarriesNeutralPostureInputAndOcclusionSemantics() {
        val hinge = AdaptiveOcclusion(
            bounds = DpRect(
                left = 390.dp,
                top = 0.dp,
                right = 410.dp,
                bottom = 800.dp,
            ),
        )
        val context = AdaptiveContext(
            posture = AdaptivePosture.Book,
            inputMode = AdaptiveInputMode.Mixed,
            occlusions = listOf(hinge),
        )

        assertEquals(AdaptivePosture.Book, context.posture)
        assertEquals(AdaptiveInputMode.Mixed, context.inputMode)
        assertEquals(listOf(hinge), context.occlusions)
    }
}
