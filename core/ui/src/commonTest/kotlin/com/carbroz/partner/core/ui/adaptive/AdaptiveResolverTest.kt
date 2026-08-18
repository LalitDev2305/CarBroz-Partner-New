package com.carbroz.partner.core.ui.adaptive

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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AdaptiveResolverTest {

    private val windowEnv = WindowEnvironment(
        windowWidth = 360.dp,
        windowHeight = 640.dp,
        density = 2.0f,
        fontScale = 1.0f
    )

    private val containerConstraints = CurrentContainerConstraints(
        availableWidth = 360.dp,
        availableHeight = 640.dp
    )

    private val horizontalContext = ResolutionContext(
        axis = ResolutionAxis.HORIZONTAL,
        window = windowEnv,
        container = containerConstraints
    )

    private val verticalContext = ResolutionContext(
        axis = ResolutionAxis.VERTICAL,
        window = windowEnv,
        container = containerConstraints
    )

    private object TestTokenResolver : DimensionTokenResolver, SpacingTokenResolver, RadiusTokenResolver, IconTokenResolver, TypographyTokenResolver {
        override fun resolveDimension(key: String): Dp? = if (key == "dimension.card_min") 120.dp else null
        override fun resolveSpacing(key: String): Dp? = if (key == "spacing.md") 16.dp else null
        override fun resolveRadius(key: String): Shape? = if (key == "radius.card") RoundedCornerShape(8.dp) else null
        override fun resolveIconSize(key: String): Dp? = if (key == "icon.md") 24.dp else null
        override fun resolveTypography(key: String): TextStyle? = if (key == "type.body") TextStyle(fontSize = 14.sp) else null
    }

    @Test
    fun verifyFixedDimensionResolution() {
        val result = DimensionResolver.resolve(DimensionSpec.Fixed(100.dp), horizontalContext)
        assertTrue(result is ResolutionResult.Resolved)
        assertEquals(ResolvedDimension.Exact(100.dp), result.value)
    }

    @Test
    fun verifyFillAndWrapSemanticPreservation() {
        val fillRes = DimensionResolver.resolve(DimensionSpec.Fill, horizontalContext)
        assertTrue(fillRes is ResolutionResult.Resolved)
        assertEquals(ResolvedDimension.Fill, fillRes.value)
        assertNotEquals(ResolvedDimension.Exact(360.dp), fillRes.value)

        val wrapRes = DimensionResolver.resolve(DimensionSpec.Wrap, horizontalContext)
        assertTrue(wrapRes is ResolutionResult.Resolved)
        assertEquals(ResolvedDimension.Wrap, wrapRes.value)
    }

    @Test
    fun verifyAdaptiveDimensionScalingAndClamping() {
        val desktopContext = ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            window = windowEnv.copy(windowWidth = 1920.dp),
            container = CurrentContainerConstraints(1920.dp, 1080.dp)
        )
        val spec = DimensionSpec.Adaptive(baseDp = 10.dp, minDp = 8.dp, maxDp = 20.dp)
        val result = DimensionResolver.resolve(spec, desktopContext)
        assertTrue(result is ResolutionResult.Resolved)
        assertEquals(ResolvedDimension.Exact(20.dp), result.value)
    }

    @Test
    fun verifyAdaptiveCalculatorInvalidInputMatrix() {
        val defaultPolicy = AdaptiveScalePolicy(referenceDp = 360.dp, dampingExponent = 0.5f, minScaleFactor = 0.8f, maxScaleFactor = 2.0f)
        
        // Available dimension variants: 0, -1, NaN, +Inf, -Inf
        val availables = listOf(0.dp, (-1).dp, Float.NaN.dp, Float.POSITIVE_INFINITY.dp, Float.NEGATIVE_INFINITY.dp)
        for (avail in availables) {
            val res = AdaptiveScaleCalculator.calculate(avail, 16.dp, defaultPolicy)
            assertTrue(!res.value.isNaN() && !res.value.isInfinite() && res.value >= 0f, "Available $avail failed calculation sanitization")
        }

        // Reference dimension variants: 0, -1, NaN, +Inf, -Inf
        val references = listOf(0.dp, (-1).dp, Float.NaN.dp, Float.POSITIVE_INFINITY.dp, Float.NEGATIVE_INFINITY.dp)
        for (ref in references) {
            val pol = defaultPolicy.copy(referenceDp = ref)
            val res = AdaptiveScaleCalculator.calculate(360.dp, 16.dp, pol)
            assertTrue(!res.value.isNaN() && !res.value.isInfinite() && res.value >= 0f, "Reference $ref failed calculation sanitization")
        }

        // Base dimension variants: 0, -1, NaN, +Inf, -Inf
        val bases = listOf(0.dp, (-1).dp, Float.NaN.dp, Float.POSITIVE_INFINITY.dp, Float.NEGATIVE_INFINITY.dp)
        for (base in bases) {
            val res = AdaptiveScaleCalculator.calculate(360.dp, base, defaultPolicy)
            assertTrue(!res.value.isNaN() && !res.value.isInfinite() && res.value >= 0f, "Base $base failed calculation sanitization")
        }

        // Damping exponent variants: -1, 0, NaN, +Inf, -Inf
        val dampings = listOf(-1.0f, 0.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        for (damp in dampings) {
            val pol = defaultPolicy.copy(dampingExponent = damp)
            val res = AdaptiveScaleCalculator.calculate(360.dp, 16.dp, pol)
            assertTrue(!res.value.isNaN() && !res.value.isInfinite() && res.value >= 0f, "Damping $damp failed calculation sanitization")
        }

        // Min/Max scale factor variants: -1, NaN, +Inf, -Inf
        val factors = listOf(-1.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        for (fac in factors) {
            val polMin = defaultPolicy.copy(minScaleFactor = fac)
            val resMin = AdaptiveScaleCalculator.calculate(360.dp, 16.dp, polMin)
            assertTrue(!resMin.value.isNaN() && !resMin.value.isInfinite() && resMin.value >= 0f, "MinScale $fac failed calculation sanitization")

            val polMax = defaultPolicy.copy(maxScaleFactor = fac)
            val resMax = AdaptiveScaleCalculator.calculate(360.dp, 16.dp, polMax)
            assertTrue(!resMax.value.isNaN() && !resMax.value.isInfinite() && resMax.value >= 0f, "MaxScale $fac failed calculation sanitization")
        }

        // Contradictory relation: minScale > maxScale
        val polContradictory = defaultPolicy.copy(minScaleFactor = 2.0f, maxScaleFactor = 0.8f)
        val resContradictory = AdaptiveScaleCalculator.calculate(360.dp, 16.dp, polContradictory)
        assertTrue(!resContradictory.value.isNaN() && !resContradictory.value.isInfinite() && resContradictory.value >= 0f, "Contradictory scale factors failed calculation sanitization")
    }

    @Test
    fun verifyAxisAwareFractionResolution() {
        val container = CurrentContainerConstraints(800.dp, 600.dp)
        val hCtx = ResolutionContext(ResolutionAxis.HORIZONTAL, windowEnv, container)
        val vCtx = ResolutionContext(ResolutionAxis.VERTICAL, windowEnv, container)

        val hResult = DimensionResolver.resolve(DimensionSpec.Fraction(0.5f), hCtx)
        val vResult = DimensionResolver.resolve(DimensionSpec.Fraction(0.5f), vCtx)

        assertEquals(ResolvedDimension.Exact(400.dp), (hResult as ResolutionResult.Resolved).value)
        assertEquals(ResolvedDimension.Exact(300.dp), (vResult as ResolutionResult.Resolved).value)
    }

    @Test
    fun verifyFractionTableDrivenMatrix() {
        val testCases = listOf(
            -0.5f to true,
            0.0f to false,
            0.5f to false,
            1.0f to false,
            1.5f to true,
            Float.NaN to true,
            Float.POSITIVE_INFINITY to true,
            Float.NEGATIVE_INFINITY to true
        )
        for ((percentage, isRecovered) in testCases) {
            val res = DimensionResolver.resolve(DimensionSpec.Fraction(percentage), horizontalContext)
            if (isRecovered) {
                assertTrue(res is ResolutionResult.Recovered, "Expected Recovered for fraction $percentage")
            } else {
                assertTrue(res is ResolutionResult.Resolved, "Expected Resolved for fraction $percentage")
            }
        }
    }

    @Test
    fun verifyDeviceWindowMatrixExecution() {
        val viewports = listOf(
            320.dp to 568.dp,
            360.dp to 800.dp,
            430.dp to 932.dp,
            600.dp to 1024.dp,
            800.dp to 1280.dp,
            800.dp to 600.dp,
            1200.dp to 800.dp,
            1920.dp to 1080.dp,
            2560.dp to 1080.dp,
            0.dp to 0.dp
        )
        for ((w, h) in viewports) {
            val env = WindowEnvironment(w, h, 2.0f, 1.0f)
            val ctx = ResolutionContext(ResolutionAxis.HORIZONTAL, env, CurrentContainerConstraints(w, h))
            val res = DimensionResolver.resolve(DimensionSpec.Adaptive(16.dp, 8.dp, 32.dp), ctx)
            assertTrue(res is ResolutionResult.Resolved)
        }
    }

    @Test
    fun verifyDesktopConstraintMatrix() {
        val spec = DimensionSpec.Fraction(0.9f)
        val constraint = LayoutConstraintSpec(maxDp = 600.dp)
        val desktopWidths = listOf(800.dp, 1200.dp, 1920.dp, 2560.dp)
        for (width in desktopWidths) {
            val ctx = ResolutionContext(ResolutionAxis.HORIZONTAL, windowEnv.copy(windowWidth = width), CurrentContainerConstraints(width, 1080.dp))
            val res = DimensionResolver.resolve(spec, ctx, constraint)
            assertEquals(ResolvedDimension.Exact(600.dp), (res as ResolutionResult.Resolved).value)
        }

        val unconstrainedCtx = ResolutionContext(ResolutionAxis.HORIZONTAL, windowEnv.copy(windowWidth = 500.dp), CurrentContainerConstraints(500.dp, 1080.dp))
        val unconstrainedRes = DimensionResolver.resolve(spec, unconstrainedCtx, constraint)
        assertEquals(ResolvedDimension.Exact(450.dp), (unconstrainedRes as ResolutionResult.Resolved).value)
    }

    @Test
    fun verifyWindowWidthClassBoundaries() {
        assertEquals(WindowWidthClass.COMPACT, WindowEnvironment(599.dp, 800.dp, 1f, 1f).widthClass)
        assertEquals(WindowWidthClass.MEDIUM, WindowEnvironment(600.dp, 800.dp, 1f, 1f).widthClass)
        assertEquals(WindowWidthClass.MEDIUM, WindowEnvironment(839.dp, 800.dp, 1f, 1f).widthClass)
        assertEquals(WindowWidthClass.EXPANDED, WindowEnvironment(840.dp, 800.dp, 1f, 1f).widthClass)
    }

    @Test
    fun verifyMinMaxContradictionProducesInvalid() {
        val constraint = LayoutConstraintSpec(minDp = 600.dp, maxDp = 400.dp)
        val result = DimensionResolver.resolve(DimensionSpec.Fixed(500.dp), horizontalContext, constraint)
        assertTrue(result is ResolutionResult.Invalid)
    }

    @Test
    fun verifyNestedContainerFractionResolution() {
        val screenCtx = ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            window = windowEnv.copy(windowWidth = 1200.dp),
            container = CurrentContainerConstraints(1200.dp, 800.dp)
        )
        val componentRes = DimensionResolver.resolve(DimensionSpec.Fraction(0.5f), screenCtx)
        val compWidth = ((componentRes as ResolutionResult.Resolved).value as ResolvedDimension.Exact).valueDp
        assertEquals(600.dp, compWidth)

        val componentCtx = screenCtx.copy(container = CurrentContainerConstraints(compWidth, 800.dp))
        val childRes = DimensionResolver.resolve(DimensionSpec.Fraction(0.5f), componentCtx)
        assertEquals(ResolvedDimension.Exact(300.dp), (childRes as ResolutionResult.Resolved).value)
    }

    @Test
    fun verifyDesignMinWidthConstraintPreservedWhenAvailableIsSmaller() {
        val narrowCtx = ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            window = windowEnv.copy(windowWidth = 320.dp),
            container = CurrentContainerConstraints(320.dp, 568.dp)
        )
        val constraint = LayoutConstraintSpec(minDp = 400.dp)
        val result = DimensionResolver.resolve(DimensionSpec.Fixed(320.dp), narrowCtx, constraint)
        assertEquals(ResolvedDimension.Exact(400.dp), (result as ResolutionResult.Resolved).value)
    }

    @Test
    fun verifyTypographyFontScaleNonDoubleScaling() {
        val type1 = TypographyResolver.resolve(TypographySpec.Fixed(16f))
        val env2 = windowEnv.copy(fontScale = 2.0f)
        val type2 = TypographyResolver.resolve(TypographySpec.Fixed(16f))
        assertEquals((type1 as ResolutionResult.Resolved).value.fontSize, (type2 as ResolutionResult.Resolved).value.fontSize)
    }

    @Test
    fun verifyNegativeAndNaNInputsSanitized() {
        val result = DimensionResolver.resolve(DimensionSpec.Fixed((-10).dp), horizontalContext)
        assertTrue(result is ResolutionResult.Recovered)
        assertEquals(ResolvedDimension.Exact(0.dp), result.fallbackValue)
    }

    @Test
    fun verifyTokenResolutionSuccessAndFallback() {
        val success = DimensionResolver.resolve(DimensionSpec.Token("dimension.card_min"), horizontalContext, tokenResolver = TestTokenResolver)
        assertTrue(success is ResolutionResult.Resolved)
        assertEquals(ResolvedDimension.Exact(120.dp), success.value)

        val fail = DimensionResolver.resolve(DimensionSpec.Token("unknown"), horizontalContext, tokenResolver = TestTokenResolver)
        assertTrue(fail is ResolutionResult.Unsupported)
    }

    @Test
    fun verifySpacingRadiusAndIconResolvers() {
        val spacingRes = SpacingResolver.resolve(SpacingSpec.Token("spacing.md"), horizontalContext, TestTokenResolver)
        assertEquals(16.dp, (spacingRes as ResolutionResult.Resolved).value)

        val iconRes = IconSizeResolver.resolve(IconSizeSpec.Token("icon.md"), horizontalContext, TestTokenResolver)
        assertEquals(24.dp, (iconRes as ResolutionResult.Resolved).value)

        val typeRes = TypographyResolver.resolve(TypographySpec.Fixed(16f))
        assertEquals(16.sp, (typeRes as ResolutionResult.Resolved).value.fontSize)
    }

    @Test
    fun verifyInteractionTargetPaddingCalculation() {
        val insets = InteractionTargetPolicy.calculatePadding(24.dp, 24.dp)
        assertEquals(12.dp, insets.start)
        assertEquals(12.dp, insets.top)
    }
}
