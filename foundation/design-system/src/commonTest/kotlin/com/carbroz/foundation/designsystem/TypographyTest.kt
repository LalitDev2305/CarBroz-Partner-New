package com.carbroz.foundation.designsystem

import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypographyTest {
    @Test
    fun defaultStylesKeepReadableLineHeights() {
        val typography = CarBrozTypography.Default

        assertTrue(typography.displayLarge.lineHeight >= typography.displayLarge.fontSize)
        assertTrue(typography.headlineLarge.lineHeight >= typography.headlineLarge.fontSize)
        assertTrue(typography.titleLarge.lineHeight >= typography.titleLarge.fontSize)
        assertTrue(typography.bodyLarge.lineHeight >= typography.bodyLarge.fontSize)
        assertTrue(typography.bodyMedium.lineHeight >= typography.bodyMedium.fontSize)
        assertTrue(typography.labelLarge.lineHeight >= typography.labelLarge.fontSize)
    }

    @Test
    fun coreBodyScaleRemainsStable() {
        val typography = CarBrozTypography.Default

        assertEquals(16.sp, typography.bodyLarge.fontSize)
        assertEquals(24.sp, typography.bodyLarge.lineHeight)
        assertEquals(14.sp, typography.bodyMedium.fontSize)
        assertEquals(20.sp, typography.bodyMedium.lineHeight)
    }
}
