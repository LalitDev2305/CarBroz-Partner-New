package com.carbroz.foundation.adaptive

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContentLayoutPolicyTest {
    @Test
    fun compactWidthClassUsesCompactPolicy() {
        assertEquals(
            ContentLayoutPolicy.Compact,
            ContentLayoutPolicyResolver.forWidthClass(AdaptiveWidthClass.Compact),
        )
    }

    @Test
    fun mediumWidthClassUsesMediumPolicy() {
        assertEquals(
            ContentLayoutPolicy.Medium,
            ContentLayoutPolicyResolver.forWidthClass(AdaptiveWidthClass.Medium),
        )
    }

    @Test
    fun expandedWidthClassUsesExpandedPolicy() {
        assertEquals(
            ContentLayoutPolicy.Expanded,
            ContentLayoutPolicyResolver.forWidthClass(AdaptiveWidthClass.Expanded),
        )
    }

    @Test
    fun maxReadableWidthMustBePositive() {
        assertFailsWith<IllegalArgumentException> {
            ContentLayoutPolicy(maxReadableWidth = 0.dp, horizontalMargin = 0.dp)
        }
    }

    @Test
    fun horizontalMarginMustNotBeNegative() {
        assertFailsWith<IllegalArgumentException> {
            ContentLayoutPolicy(maxReadableWidth = 1.dp, horizontalMargin = (-1).dp)
        }
    }
}
