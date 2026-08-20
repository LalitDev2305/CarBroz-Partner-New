package com.carbroz.partner.sdui.render.resolver

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
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

    private val rootContext = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = CurrentContainerConstraints(availableWidth = 800.dp, availableHeight = 1200.dp)
    )

    private val localParentContext = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = CurrentContainerConstraints(availableWidth = 400.dp, availableHeight = 600.dp)
    )

    private val smallerLocalParentContext = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = CurrentContainerConstraints(availableWidth = 200.dp, availableHeight = 300.dp)
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
    fun test1_FixedWidthResolvesCorrectly() {
        val res = LayoutResolver.resolveWidth(DimensionSpec.Fixed(100.dp), localParentContext)
        assertTrue(res is ResolutionResult.Resolved)
    }

    @Test
    fun test2_FixedHeightResolvesCorrectly() {
        val res = LayoutResolver.resolveHeight(DimensionSpec.Fixed(100.dp), localParentContext)
        assertTrue(res is ResolutionResult.Resolved)
    }

    @Test
    fun test3_FractionResolvesFromSuppliedLocalContainer() {
        val spec = DimensionSpec.Fraction(0.5f)
        val rootRes = LayoutResolver.resolveWidth(spec, rootContext)
        val localRes = LayoutResolver.resolveWidth(spec, localParentContext)
        assertTrue(rootRes is ResolutionResult.Resolved)
        assertTrue(localRes is ResolutionResult.Resolved)
        assertNotEquals(rootRes.value, localRes.value)
    }

    @Test
    fun test4_AdaptiveResolvesFromSuppliedLocalContainer() {
        val spec = DimensionSpec.Adaptive(100.dp)
        val rootRes = LayoutResolver.resolveWidth(spec, rootContext)
        val localRes = LayoutResolver.resolveWidth(spec, localParentContext)
        assertTrue(rootRes is ResolutionResult.Resolved)
        assertTrue(localRes is ResolutionResult.Resolved)
        assertNotEquals(rootRes.value, localRes.value)
    }

    @Test
    fun test5_FillPreserved() {
        val res = LayoutResolver.resolveWidth(DimensionSpec.Fill, localParentContext)
        assertTrue(res is ResolutionResult.Resolved)
    }

    @Test
    fun test6_WrapPreserved() {
        val res = LayoutResolver.resolveWidth(DimensionSpec.Wrap, localParentContext)
        assertTrue(res is ResolutionResult.Resolved)
        assertEquals(Modifier, res.value)
    }

    @Test
    fun test7_TokenDimensionResolverSuccess() {
        val spec = DimensionSpec.Token("custom_dim")
        val res = LayoutResolver.resolveWidth(spec, localParentContext, tokenResolver = DummyDimensionTokenResolver)
        assertTrue(res is ResolutionResult.Resolved)
    }

    @Test
    fun test8_MissingTokenStaysUnsupported() {
        val spec = DimensionSpec.Token("unresolved_key")
        val res = LayoutResolver.resolveWidth(spec, localParentContext)
        assertTrue(res is ResolutionResult.Unsupported)
        assertEquals("unresolved_key", res.tokenKey)
    }

    @Test
    fun test9_AdaptiveRowGapUsesHorizontalLocalConstraint() {
        val spec = SpacingSpec.Adaptive(16.dp)
        val hContext = ResolutionContext(ResolutionAxis.HORIZONTAL, CurrentContainerConstraints(400.dp, 800.dp))
        val res = LayoutResolver.resolveGap(spec, hContext)
        assertTrue(res is ResolutionResult.Resolved)
    }

    @Test
    fun test10_AdaptiveColumnGapUsesVerticalLocalConstraint() {
        val spec = SpacingSpec.Adaptive(16.dp)
        val vContext = ResolutionContext(ResolutionAxis.VERTICAL, CurrentContainerConstraints(400.dp, 800.dp))
        val res = LayoutResolver.resolveGap(spec, vContext)
        assertTrue(res is ResolutionResult.Resolved)
    }

    @Test
    fun test11_TokenGapSuccess() {
        val spec = SpacingSpec.Token("custom_gap")
        val res = LayoutResolver.resolveGap(spec, localParentContext, tokenResolver = DummySpacingTokenResolver)
        assertTrue(res is ResolutionResult.Resolved)
        assertEquals(24.dp, res.value)
    }

    @Test
    fun test12_MissingTokenGapStaysUnsupported() {
        val spec = SpacingSpec.Token("unresolved_gap")
        val res = LayoutResolver.resolveGap(spec, localParentContext)
        assertTrue(res is ResolutionResult.Unsupported)
        assertEquals("unresolved_gap", res.tokenKey)
    }

    @Test
    fun test17_NestedFractionUsesParentContainerNotRoot() {
        val spec = DimensionSpec.Fraction(0.5f)
        val localRes = LayoutResolver.resolveWidth(spec, localParentContext)
        val smallerRes = LayoutResolver.resolveWidth(spec, smallerLocalParentContext)
        assertTrue(localRes is ResolutionResult.Resolved)
        assertTrue(smallerRes is ResolutionResult.Resolved)
        assertNotEquals(localRes.value, smallerRes.value)
    }

    @Test
    fun test18_NestedAdaptiveUsesParentContainerNotRoot() {
        val spec = DimensionSpec.Adaptive(100.dp)
        val localRes = LayoutResolver.resolveWidth(spec, localParentContext)
        val smallerRes = LayoutResolver.resolveWidth(spec, smallerLocalParentContext)
        assertTrue(localRes is ResolutionResult.Resolved)
        assertTrue(smallerRes is ResolutionResult.Resolved)
        assertNotEquals(localRes.value, smallerRes.value)
    }

    @Test
    fun testAdaptiveRowGapUsesNestedContainerAvailableWidthNotRoot() {
        val rContext = ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            container = CurrentContainerConstraints(availableWidth = 400.dp, availableHeight = 800.dp)
        )
        val nestedContext = ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            container = CurrentContainerConstraints(availableWidth = 200.dp, availableHeight = 400.dp)
        )

        val spec = SpacingSpec.Adaptive(16.dp)
        val rootGapRes = LayoutResolver.resolveGap(spec, rContext)
        val nestedGapRes = LayoutResolver.resolveGap(spec, nestedContext)

        assertTrue(rootGapRes is ResolutionResult.Resolved)
        assertTrue(nestedGapRes is ResolutionResult.Resolved)
        assertNotEquals(rootGapRes.value, nestedGapRes.value)
    }

    @Test
    fun testAdaptiveColumnGapUsesNestedContainerAvailableHeightNotRoot() {
        val rContext = ResolutionContext(
            axis = ResolutionAxis.VERTICAL,
            container = CurrentContainerConstraints(availableWidth = 400.dp, availableHeight = 800.dp)
        )
        val nestedContext = ResolutionContext(
            axis = ResolutionAxis.VERTICAL,
            container = CurrentContainerConstraints(availableWidth = 200.dp, availableHeight = 400.dp)
        )

        val spec = SpacingSpec.Adaptive(16.dp)
        val rootGapRes = LayoutResolver.resolveGap(spec, rContext)
        val nestedGapRes = LayoutResolver.resolveGap(spec, nestedContext)

        assertTrue(rootGapRes is ResolutionResult.Resolved)
        assertTrue(nestedGapRes is ResolutionResult.Resolved)
        assertNotEquals(rootGapRes.value, nestedGapRes.value)
    }
}
