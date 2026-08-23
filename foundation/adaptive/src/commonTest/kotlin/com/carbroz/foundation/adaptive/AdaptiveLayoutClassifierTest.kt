package com.carbroz.foundation.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdaptiveLayoutClassifierTest {
    @Test
    fun compactBelowWidthAndHeightBreakpoints() {
        val info = AdaptiveLayoutClassifier.classify(width = 599.dp, height = 479.dp)

        assertEquals(AdaptiveWidthClass.Compact, info.widthClass)
        assertEquals(AdaptiveHeightClass.Compact, info.heightClass)
    }

    @Test
    fun mediumAtExactFirstBreakpoints() {
        val info = AdaptiveLayoutClassifier.classify(width = 600.dp, height = 480.dp)

        assertEquals(AdaptiveWidthClass.Medium, info.widthClass)
        assertEquals(AdaptiveHeightClass.Medium, info.heightClass)
    }

    @Test
    fun mediumBelowExpandedBreakpoints() {
        val info = AdaptiveLayoutClassifier.classify(width = 839.dp, height = 899.dp)

        assertEquals(AdaptiveWidthClass.Medium, info.widthClass)
        assertEquals(AdaptiveHeightClass.Medium, info.heightClass)
    }

    @Test
    fun expandedAtExactExpandedBreakpoints() {
        val info = AdaptiveLayoutClassifier.classify(width = 840.dp, height = 900.dp)

        assertEquals(AdaptiveWidthClass.Expanded, info.widthClass)
        assertEquals(AdaptiveHeightClass.Expanded, info.heightClass)
    }

    @Test
    fun zeroSizeIsValidAndCompact() {
        val info = AdaptiveLayoutClassifier.classify(width = 0.dp, height = 0.dp)

        assertEquals(AdaptiveWidthClass.Compact, info.widthClass)
        assertEquals(AdaptiveHeightClass.Compact, info.heightClass)
    }

    @Test
    fun negativeAvailableSizeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            AdaptiveLayoutClassifier.classify(width = (-1).dp, height = 100.dp)
        }
        assertFailsWith<IllegalArgumentException> {
            AdaptiveLayoutClassifier.classify(width = 100.dp, height = (-1).dp)
        }
    }

    @Test
    fun unspecifiedAvailableSizeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            AdaptiveLayoutClassifier.classify(width = Dp.Unspecified, height = 100.dp)
        }
        assertFailsWith<IllegalArgumentException> {
            AdaptiveLayoutClassifier.classify(width = 100.dp, height = Dp.Unspecified)
        }
    }

    @Test
    fun infiniteAvailableSizeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            AdaptiveLayoutClassifier.classify(width = Dp.Infinity, height = 100.dp)
        }
        assertFailsWith<IllegalArgumentException> {
            AdaptiveLayoutClassifier.classify(width = 100.dp, height = Dp.Infinity)
        }
    }
}
