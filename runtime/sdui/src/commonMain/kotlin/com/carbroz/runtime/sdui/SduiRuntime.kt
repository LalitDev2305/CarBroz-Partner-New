package com.carbroz.runtime.sdui

import com.carbroz.runtime.sdui.compatibility.SduiClientCompatibility
import com.carbroz.runtime.sdui.compatibility.SduiCompatibilityPolicy
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
import com.carbroz.runtime.sdui.protocol.SduiDecoder
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.registry.SduiRegistry
import com.carbroz.runtime.sdui.registry.SduiRegistryFactory
import com.carbroz.runtime.sdui.rendering.SduiRendererDispatcher

/**
 * Canonical SDUI runtime owned by runtime:sdui.
 *
 * This object contains only generic SDUI concerns: definitions, decoding, validation,
 * compatibility, normalization and rendering. Feature lifecycle, navigation and action
 * execution deliberately remain outside this module.
 */
data class SduiRuntime(
    val registry: SduiRegistry,
    val pipeline: SduiPipeline,
    val renderer: SduiRendererDispatcher,
)

object SduiRuntimeFactory {
    fun createCore(clientCompatibility: SduiClientCompatibility): SduiRuntime {
        val registry = SduiRegistryFactory.createCore()
        val decoder = SduiDecoder()
        val validator = SduiSchemaValidator()
        val normalizer = SduiNormalizer(registry)
        val pipeline = SduiPipeline(
            decoder = decoder,
            validator = validator,
            compatibilityPolicy = SduiCompatibilityPolicy(
                client = clientCompatibility,
                registry = registry,
            ),
            normalizer = normalizer,
        )
        return SduiRuntime(
            registry = registry,
            pipeline = pipeline,
            renderer = SduiRendererDispatcher(registry),
        )
    }
}
