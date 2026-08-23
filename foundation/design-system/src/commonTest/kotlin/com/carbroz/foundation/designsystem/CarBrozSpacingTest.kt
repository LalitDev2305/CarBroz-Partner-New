package com.carbroz.foundation.designsystem

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CarBrozSpacingTest {
    @Test
    fun defaultScaleIsNonDecreasing() {
        val spacing = CarBrozSpacing.Default

        assertEquals(0.dp, spacing.none)
        assertEquals(4.dp, spacing.extraSmall)
        assertEquals(8.dp, spacing.small)
        assertEquals(16.dp, spacing.medium)
        assertEquals(24.dp, spacing.large)
        assertEquals(32.dp, spacing.extraLarge)
    }

    @Test
    fun negativeSpacingIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            CarBrozSpacing(
                none = (-1).dp,
                extraSmall = 4.dp,
                small = 8.dp,
                medium = 16.dp,
                large = 24.dp,
                extraLarge = 32.dp,
            )
        }
    }

    @Test
    fun decreasingScaleIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            CarBrozSpacing(
                none = 0.dp,
                extraSmall = 8.dp,
                small = 4.dp,
                medium = 16.dp,
                large = 24.dp,
                extraLarge = 32.dp,
            )
        }
    }
}
