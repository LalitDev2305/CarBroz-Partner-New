package com.carbroz.partner.sdui.render.resolver

import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.sdui.engine.model.LayoutAlignment
import com.carbroz.partner.sdui.engine.model.LayoutArrangement
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutResolverTest {

    @Test
    fun testEdgeSpacingResolution() {
        val edge = SduiEdgeSpacing(top = 10, bottom = 20, start = 5, end = 15)
        val paddingValues = LayoutResolver.resolveEdgeSpacing(edge)
        assertEquals(5.dp, paddingValues.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr))
        assertEquals(10.dp, paddingValues.calculateTopPadding())
        assertEquals(15.dp, paddingValues.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr))
        assertEquals(20.dp, paddingValues.calculateBottomPadding())
    }

    @Test
    fun testGapResolution() {
        val gapFixed = SpacingSpec.Fixed(16.dp)
        assertEquals(16.dp, LayoutResolver.resolveGap(gapFixed).value)
    }
}
