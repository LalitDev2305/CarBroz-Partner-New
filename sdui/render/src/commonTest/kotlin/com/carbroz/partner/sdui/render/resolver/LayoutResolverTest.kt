package com.carbroz.partner.sdui.render.resolver

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.core.ui.tokens.DimensionTokenResolver
import com.carbroz.partner.core.ui.tokens.SpacingTokenResolver
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LayoutResolverTest {

    private val customContext = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = CurrentContainerConstraints(availableWidth = 720.dp, availableHeight = 1280.dp)
    )

    private object DummySpacingTokenResolver : SpacingTokenResolver {
        override fun resolveSpacing(key: String): androidx.compose.ui.unit.Dp? =
            if (key == "custom_gap") 24.dp else null
    }

    private object DummyDimensionTokenResolver : DimensionTokenResolver {
        override fun resolveDimension(key: String): androidx.compose.ui.unit.Dp? =
            if (key == "custom_dim") 50.dp else null
    }

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
    fun testAdaptiveWidthUsesCoreUiAdaptiveCalculation() {
        val spec = DimensionSpec.Adaptive(100.dp)
        val modifier = LayoutResolver.resolveWidth(spec, customContext)
        assertNotEquals(Modifier, modifier)
    }

    @Test
    fun testAdaptiveHeightUsesCoreUiAdaptiveCalculation() {
        val spec = DimensionSpec.Adaptive(100.dp)
        val vContext = customContext.copy(axis = ResolutionAxis.VERTICAL)
        val modifier = LayoutResolver.resolveHeight(spec, vContext)
        assertNotEquals(Modifier, modifier)
    }

    @Test
    fun testTokenDimensionDoesNotBecomeFillMax() {
        val spec = DimensionSpec.Token("unresolved_key")
        val modifier = LayoutResolver.resolveWidth(spec)
        assertEquals(Modifier, modifier)
    }

    @Test
    fun testAdaptiveGapUsesCalculatedAdaptiveSpacing() {
        val spec = SpacingSpec.Adaptive(16.dp)
        val gap = LayoutResolver.resolveGap(spec, customContext)
        assertNotEquals(16.dp, gap.value)
    }

    @Test
    fun testTokenGapDoesNotBecomeHardcoded16Dp() {
        val spec = SpacingSpec.Token("unresolved_gap")
        val gapWithoutResolver = LayoutResolver.resolveGap(spec)
        assertEquals(0.dp, gapWithoutResolver.value)

        val specResolved = SpacingSpec.Token("custom_gap")
        val gapWithResolver = LayoutResolver.resolveGap(specResolved, tokenResolver = DummySpacingTokenResolver)
        assertEquals(24.dp, gapWithResolver.value)
    }
}
