package com.carbroz.runtime.sdui

import com.carbroz.runtime.sdui.compatibility.SduiCompatibilityPolicy
import com.carbroz.runtime.sdui.compatibility.SduiCompatibilityResult
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.normalization.SduiNormalizationResult
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
import com.carbroz.runtime.sdui.protocol.SduiDecodeResult
import com.carbroz.runtime.sdui.protocol.SduiDecoder
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.protocol.SduiValidationResult

/**
 * Canonical entry point from untrusted SDUI JSON to trusted immutable runtime IR.
 *
 * Callers must not need to know or reproduce the mandatory processing order. A payload
 * can reach [SduiPipelineResult.Success] only after decode, structural validation,
 * compatibility evaluation and normalization all succeed.
 */
class SduiPipeline(
    private val decoder: SduiDecoder,
    private val validator: SduiSchemaValidator,
    private val compatibilityPolicy: SduiCompatibilityPolicy,
    private val normalizer: SduiNormalizer,
) {
    fun process(payload: String): SduiPipelineResult {
        val envelope = when (val decoded = decoder.decode(payload)) {
            is SduiDecodeResult.Success -> decoded.envelope
            is SduiDecodeResult.Failure -> return SduiPipelineResult.DecodeFailure(decoded.error)
        }

        when (val validation = validator.validate(envelope)) {
            SduiValidationResult.Valid -> Unit
            is SduiValidationResult.Invalid ->
                return SduiPipelineResult.ValidationFailure(validation.violations)
        }

        when (val compatibility = compatibilityPolicy.evaluate(envelope)) {
            SduiCompatibilityResult.Compatible -> Unit
            is SduiCompatibilityResult.Incompatible ->
                return SduiPipelineResult.CompatibilityFailure(compatibility.issues)
        }

        return when (val normalization = normalizer.normalize(envelope)) {
            is SduiNormalizationResult.Success -> SduiPipelineResult.Success(normalization.screen)
            is SduiNormalizationResult.Failure ->
                SduiPipelineResult.NormalizationFailure(normalization.error)
        }
    }
}

sealed interface SduiPipelineResult {
    data class Success(val screen: Screen) : SduiPipelineResult
    data class DecodeFailure(
        val error: com.carbroz.runtime.sdui.protocol.SduiDecodeError,
    ) : SduiPipelineResult

    data class ValidationFailure(
        val violations: List<com.carbroz.runtime.sdui.protocol.SduiViolation>,
    ) : SduiPipelineResult

    data class CompatibilityFailure(
        val issues: List<com.carbroz.runtime.sdui.compatibility.SduiCompatibilityIssue>,
    ) : SduiPipelineResult

    data class NormalizationFailure(
        val error: com.carbroz.runtime.sdui.normalization.SduiNormalizationError,
    ) : SduiPipelineResult
}
