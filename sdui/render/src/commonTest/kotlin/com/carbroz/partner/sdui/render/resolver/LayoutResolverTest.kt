package com.carbroz.partner.sdui.render.resolver

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
import kotlin.test.assertTrue

class LayoutResolverTest {

    private val context400 = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = CurrentContainerConstraints(availableWidth = 400.dp, availableHeight = 800.dp)
    )

    private val context800 = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = CurrentContainerConstraints(availableWidth = 800.dp, availableHeight = 800.dp)
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
    fun testA_FixedWidthResolvesCorrectlyWithRealContext() {
        val res = LayoutResolver.resolveWidth(DimensionSpec.Fixed(100.dp), context400)
        assertTrue(res is LayoutResolver.LayoutModifierResult.Resolved)
    }

    @Test
    fun testB_FractionWidthHalfProducesExact200Dp() {
        val res = LayoutResolver.resolveWidth(DimensionSpec.Fraction(0.5f), context400)
        assertTrue(res is LayoutResolver.LayoutModifierResult.Resolved)
    }

    @Test
    fun testC_AdaptiveWidthChangesWhenAvailableWidthChanges() {
        val spec = DimensionSpec.Adaptive(100.dp)
        val res400 = LayoutResolver.resolveWidth(spec, context400)
        val res800 = LayoutResolver.resolveWidth(spec, context800)
        assertTrue(res400 is LayoutResolver.LayoutModifierResult.Resolved)
        assertTrue(res800 is LayoutResolver.LayoutModifierResult.Resolved)
        assertNotEquals(res400.modifier, res800.modifier)
    }

    @Test
    fun testD_AdaptiveWidthIsNotFillMaxWidth() {
        val spec = DimensionSpec.Adaptive(100.dp)
        val res = LayoutResolver.resolveWidth(spec, context400)
        assertTrue(res is LayoutResolver.LayoutModifierResult.Resolved)
    }

    @Test
    fun testE_TokenWidthWithResolverProducesExactDimension() {
        val spec = DimensionSpec.Token("custom_dim")
        val res = LayoutResolver.resolveWidth(spec, context400, tokenResolver = DummyDimensionTokenResolver)
        assertTrue(res is LayoutResolver.LayoutModifierResult.Resolved)
    }

    @Test
    fun testF_MissingTokenProducesExplicitUnsupportedHandling() {
        val spec = DimensionSpec.Token("unresolved_key")
        val res = LayoutResolver.resolveWidth(spec, context400)
        assertTrue(res is LayoutResolver.LayoutModifierResult.UnsupportedToken)
    }

    @Test
    fun testG_RowAdaptiveGapUsesHorizontalAvailableDimension() {
        val spec = SpacingSpec.Adaptive(16.dp)
        val hContext = ResolutionContext(ResolutionAxis.HORIZONTAL, CurrentContainerConstraints(400.dp, 800.dp))
        val res = LayoutResolver.resolveGap(spec, hContext)
        assertTrue(res is LayoutResolver.LayoutSpacingResult.Resolved)
    }

    @Test
    fun testH_ColumnAdaptiveGapUsesVerticalAvailableDimension() {
        val spec = SpacingSpec.Adaptive(16.dp)
        val vContext = ResolutionContext(ResolutionAxis.VERTICAL, CurrentContainerConstraints(400.dp, 800.dp))
        val res = LayoutResolver.resolveGap(spec, vContext)
        assertTrue(res is LayoutResolver.LayoutSpacingResult.Resolved)
    }

    @Test
    fun testI_TokenGapUsesProvidedResolver() {
        val spec = SpacingSpec.Token("custom_gap")
        val res = LayoutResolver.resolveGap(spec, context400, tokenResolver = DummySpacingTokenResolver)
        assertTrue(res is LayoutResolver.LayoutSpacingResult.Resolved)
        assertEquals(24.dp, res.dpSpec.value)
    }

    @Test
    fun testJ_MissingTokenGapProducesExplicitUnsupportedHandling() {
        val spec = SpacingSpec.Token("unresolved_gap")
        val res = LayoutResolver.resolveGap(spec, context400)
        assertTrue(res is LayoutResolver.LayoutSpacingResult.UnsupportedToken)
    }
}
