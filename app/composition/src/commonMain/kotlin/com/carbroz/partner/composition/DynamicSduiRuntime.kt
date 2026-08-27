package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.runtime.action.ActionPreparer
import com.carbroz.runtime.action.ActionRegistry
import com.carbroz.runtime.action.CoreActionDefinitions
import com.carbroz.runtime.sdui.SduiPipeline
import com.carbroz.runtime.sdui.compatibility.SduiClientCompatibility
import com.carbroz.runtime.sdui.compatibility.SduiCompatibilityPolicy
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
import com.carbroz.runtime.sdui.protocol.SduiDecoder
import com.carbroz.runtime.sdui.protocol.SduiSchemaValidator
import com.carbroz.runtime.sdui.registry.SduiRegistry
import com.carbroz.runtime.sdui.registry.SduiRegistryFactory
import com.carbroz.runtime.sdui.rendering.SduiRendererDispatcher

/** Immutable application wiring for every backend-driven SDUI screen. */
data class DynamicSduiRuntime(
    val registry: SduiRegistry,
    val pipeline: SduiPipeline,
    val decoder: SduiDecoder,
    val validator: SduiSchemaValidator,
    val normalizer: SduiNormalizer,
    val renderer: SduiRendererDispatcher,
    val actions: ActionPreparer,
)

internal fun createDynamicSduiRuntime(configuration: AppConfiguration): DynamicSduiRuntime {
    val registry = SduiRegistryFactory.createCore()
    val decoder = SduiDecoder()
    val validator = SduiSchemaValidator()
    val normalizer = SduiNormalizer(registry)
    val pipeline = SduiPipeline(
        decoder = decoder,
        validator = validator,
        compatibilityPolicy = SduiCompatibilityPolicy(
            client = SduiClientCompatibility(
                clientVersion = configuration.buildInformation.versionCode.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt(),
                supportedProtocolVersions = 1..1,
                supportedSchemaVersions = 1..1,
            ),
            registry = registry,
        ),
        normalizer = normalizer,
    )
    val actionRegistry = ActionRegistry.builder().registerAll(CoreActionDefinitions.all).build()
    return DynamicSduiRuntime(
        registry = registry,
        pipeline = pipeline,
        decoder = decoder,
        validator = validator,
        normalizer = normalizer,
        renderer = SduiRendererDispatcher(registry),
        actions = ActionPreparer(actionRegistry),
    )
}
