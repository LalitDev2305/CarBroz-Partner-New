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
import com.carbroz.runtime.sdui.registry.CoreSduiDefinitions
import com.carbroz.runtime.sdui.registry.SduiRegistryBuilder
import com.carbroz.runtime.sdui.rendering.SduiRendererDispatcher

/** Immutable application wiring around the frozen SDUI and action runtimes. */
data class ReferenceSduiRuntime(
    val pipeline: SduiPipeline,
    val renderer: SduiRendererDispatcher,
    val actions: ActionPreparer,
)

internal fun createReferenceSduiRuntime(
    configuration: AppConfiguration,
): ReferenceSduiRuntime {
    val registry = SduiRegistryBuilder().apply {
        registerAll(CoreSduiDefinitions.all)
    }.build()

    val pipeline = SduiPipeline(
        decoder = SduiDecoder(),
        validator = SduiSchemaValidator(),
        compatibilityPolicy = SduiCompatibilityPolicy(
            client = SduiClientCompatibility(
                clientVersion = configuration.buildInformation.versionCode
                    .coerceIn(1L, Int.MAX_VALUE.toLong())
                    .toInt(),
                supportedProtocolVersions = 1..1,
                supportedSchemaVersions = 1..1,
            ),
            registry = registry,
        ),
        normalizer = SduiNormalizer(registry),
    )

    val actionRegistry = ActionRegistry.builder()
        .registerAll(CoreActionDefinitions.all)
        .build()

    return ReferenceSduiRuntime(
        pipeline = pipeline,
        renderer = SduiRendererDispatcher(registry),
        actions = ActionPreparer(actionRegistry),
    )
}
