package com.carbroz.partner.core.ui.adaptive.resolver

import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.core.ui.adaptive.result.ResolvedDimension
import com.carbroz.partner.core.ui.adaptive.scaling.AdaptiveScaleCalculator
import com.carbroz.partner.core.ui.adaptive.scaling.AdaptiveScalePolicy
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.IconSizeSpec
import com.carbroz.partner.core.ui.adaptive.spec.LayoutConstraintSpec
import com.carbroz.partner.core.ui.adaptive.spec.RadiusSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.core.ui.adaptive.spec.TypographySpec
import com.carbroz.partner.core.ui.tokens.DimensionTokenResolver
import com.carbroz.partner.core.ui.tokens.IconTokenResolver
import com.carbroz.partner.core.ui.tokens.RadiusTokenResolver
import com.carbroz.partner.core.ui.tokens.SpacingTokenResolver
import com.carbroz.partner.core.ui.tokens.TypographyTokenResolver
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdaptiveResolverTest {

    private val containerConstraints = CurrentContainerConstraints(
        availableWidth = 360.dp,
        availableHeight = 640.dp
    )

    private val horizontalContext = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        container = containerConstraints
    )

    private val verticalContext = ResolutionContext(
        axis = ResolutionAxis.VERTICAL,
        container = containerConstraints
    )

    private object TestTokenResolver : DimensionTokenResolver, SpacingTokenResolver, RadiusTokenResolver, IconTokenResolver, TypographyTokenResolver {
        override fun resolveDimension(key: String): Dp? = if (key == "dimension.card_min") 120.dp else null
        override fun resolveSpacing(key: String): Dp? = if (key == "spacing.md") 16.dp else null
        override fun resolveRadius(key: String): Shape? = if (key == "radius.card") RoundedCornerShape(8.dp) else null
        override fun resolveIconSize(key: String): Dp? = if (key == "icon.md") 24.dp else null
        override fun resolveTypography(key: String): TextStyle? = if (key == "type.body") TextStyle(fontSize = 14.sp) else null
    }

    // --- Invariant Tests ---

    @Test
    fun testDimensionSpecInvariants() {
        assertFailsWith<IllegalArgumentException> { DimensionSpec.Fixed((-10).dp) }
        assertFailsWith<IllegalArgumentException> { DimensionSpec.Adaptive((-1).dp) }
        assertFailsWith<IllegalArgumentException> { DimensionSpec.Adaptive(10.dp, minDp = 20.dp, maxDp = 10.dp) }
        assertFailsWith<IllegalArgumentException> { DimensionSpec.Fraction(-0.1f) }
        assertFailsWith<IllegalArgumentException> { DimensionSpec.Fraction(1.5f) }
        assertFailsWith<IllegalArgumentException> { DimensionSpec.Token("  ") }
    }

    @Test
    fun testSpacingSpecInvariants() {
        assertFailsWith<IllegalArgumentException> { SpacingSpec.Fixed((-5).dp) }
        assertFailsWith<IllegalArgumentException> { SpacingSpec.Adaptive((-5).dp) }
        assertFailsWith<IllegalArgumentException> { SpacingSpec.Token("") }
    }

    @Test
    fun testIconSizeSpecInvariants() {
        assertFailsWith<IllegalArgumentException> { IconSizeSpec.Fixed((-5).dp) }
        assertFailsWith<IllegalArgumentException> { IconSizeSpec.Adaptive((-5).dp) }
        assertFailsWith<IllegalArgumentException> { IconSizeSpec.Token("") }
    }

    @Test
    fun testRadiusSpecInvariants() {
        assertFailsWith<IllegalArgumentException> { RadiusSpec.Fixed((-5).dp) }
        assertFailsWith<IllegalArgumentException> { RadiusSpec.Token("") }
    }

    @Test
    fun testTypographySpecInvariants() {
        assertFailsWith<IllegalArgumentException> { TypographySpec.Fixed(0f) }
        assertFailsWith<IllegalArgumentException> { TypographySpec.Fixed(-10f) }
        assertFailsWith<IllegalArgumentException> { TypographySpec.Token("") }
    }

    @Test
    fun testLayoutConstraintSpecInvariants() {
        assertFailsWith<IllegalArgumentException> { LayoutConstraintSpec(minDp = 100.dp, maxDp = 50.dp) }
        assertFailsWith<IllegalArgumentException> { LayoutConstraintSpec(minDp = (-1).dp) }
    }

    @Test
    fun testAdaptiveScalePolicyInvariants() {
        assertFailsWith<IllegalArgumentException> { AdaptiveScalePolicy(referenceDp = 0.dp) }
        assertFailsWith<IllegalArgumentException> { AdaptiveScalePolicy(referenceDp = 360.dp, minScaleFactor = 2f, maxScaleFactor = 1f) }
    }

    // --- Resolver Tests ---

    @Test
    fun testDimensionResolverFixedFillWrapFractionToken() {
        assertEquals(
            ResolutionResult.Resolved(ResolvedDimension.Exact(100.dp)),
            DimensionResolver.resolve(DimensionSpec.Fixed(100.dp), horizontalContext)
        )
        assertEquals(
            ResolutionResult.Resolved(ResolvedDimension.Fill),
            DimensionResolver.resolve(DimensionSpec.Fill, horizontalContext)
        )
        assertEquals(
            ResolutionResult.Resolved(ResolvedDimension.Wrap),
            DimensionResolver.resolve(DimensionSpec.Wrap, horizontalContext)
        )
        assertEquals(
            ResolutionResult.Resolved(ResolvedDimension.Exact(180.dp)),
            DimensionResolver.resolve(DimensionSpec.Fraction(0.5f), horizontalContext)
        )
        assertEquals(
            ResolutionResult.Resolved(ResolvedDimension.Exact(320.dp)),
            DimensionResolver.resolve(DimensionSpec.Fraction(0.5f), verticalContext)
        )
        assertEquals(
            ResolutionResult.Resolved(ResolvedDimension.Exact(120.dp)),
            DimensionResolver.resolve(DimensionSpec.Token("dimension.card_min"), horizontalContext, tokenResolver = TestTokenResolver)
        )
        assertEquals(
            ResolutionResult.Unsupported("unknown.token"),
            DimensionResolver.resolve(DimensionSpec.Token("unknown.token"), horizontalContext, tokenResolver = TestTokenResolver)
        )
    }

    @Test
    fun testDimensionResolverConstraints() {
        val constraint = LayoutConstraintSpec(minDp = 150.dp, maxDp = 300.dp)
        val resUnder = DimensionResolver.resolve(DimensionSpec.Fixed(100.dp), horizontalContext, constraint = constraint)
        assertEquals(ResolutionResult.Resolved(ResolvedDimension.Exact(150.dp)), resUnder)

        val resOver = DimensionResolver.resolve(DimensionSpec.Fixed(400.dp), horizontalContext, constraint = constraint)
        assertEquals(ResolutionResult.Resolved(ResolvedDimension.Exact(300.dp)), resOver)
    }

    @Test
    fun testSpacingResolver() {
        assertEquals(
            ResolutionResult.Resolved(16.dp),
            SpacingResolver.resolve(SpacingSpec.Fixed(16.dp), horizontalContext)
        )
        val tokenRes = SpacingResolver.resolve(SpacingSpec.Token("spacing.md"), horizontalContext, TestTokenResolver)
        assertEquals(ResolutionResult.Resolved(16.dp), tokenRes)

        val unsuppRes = SpacingResolver.resolve(SpacingSpec.Token("unknown"), horizontalContext, TestTokenResolver)
        assertEquals(ResolutionResult.Unsupported("unknown"), unsuppRes)
    }

    @Test
    fun testIconSizeResolver() {
        assertEquals(
            ResolutionResult.Resolved(24.dp),
            IconSizeResolver.resolve(IconSizeSpec.Fixed(24.dp), horizontalContext)
        )
        val tokenRes = IconSizeResolver.resolve(IconSizeSpec.Token("icon.md"), horizontalContext, TestTokenResolver)
        assertEquals(ResolutionResult.Resolved(24.dp), tokenRes)
    }

    @Test
    fun testRadiusResolver() {
        val fixedRes = RadiusResolver.resolve(RadiusSpec.Fixed(8.dp))
        assertTrue(fixedRes is ResolutionResult.Resolved)

        val tokenRes = RadiusResolver.resolve(RadiusSpec.Token("radius.card"), TestTokenResolver)
        assertTrue(tokenRes is ResolutionResult.Resolved)
    }

    @Test
    fun testTypographyResolver() {
        val fixedRes = TypographyResolver.resolve(TypographySpec.Fixed(16f))
        assertEquals(ResolutionResult.Resolved(TextStyle(fontSize = 16.sp)), fixedRes)

        val tokenRes = TypographyResolver.resolve(TypographySpec.Token("type.body"), TestTokenResolver)
        assertEquals(ResolutionResult.Resolved(TextStyle(fontSize = 14.sp)), tokenRes)
    }

    @Test
    fun testAdaptiveScaleCalculatorZeroAvailableFallback() {
        val policy = AdaptiveScalePolicy(referenceDp = 360.dp)
        val res = AdaptiveScaleCalculator.calculate(0.dp, 16.dp, policy)
        assertEquals(16.dp, res)
    }
}
