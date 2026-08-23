package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Untrusted transport envelope. Never render or execute this model directly. */
@Serializable
data class SduiEnvelopeDto(
    val protocolVersion: Int,
    val schemaVersion: Int,
    val minimumClientVersion: Int = 1,
    val screen: ScreenDto,
    val requiredRenderers: Set<String> = emptySet(),
    val requiredCapabilities: Set<String> = emptySet(),
)

@Serializable
data class ScreenDto(
    val id: String,
    val version: Int,
    val template: TemplateDto,
)

@Serializable
data class TemplateDto(
    val id: String,
    val type: String,
    val components: List<ComponentDto> = emptyList(),
    val children: List<ChildDto> = emptyList(),
)

@Serializable
data class ComponentDto(
    val id: String,
    val type: String,
    val subComponents: List<SubComponentDto> = emptyList(),
    val children: List<ChildDto> = emptyList(),
)

@Serializable
data class SubComponentDto(
    val id: String,
    val type: String,
    val children: List<ChildDto> = emptyList(),
)

@Serializable
data class ChildDto(
    val id: String,
    val type: String,
    val data: List<ChildDataDto>,
)

/**
 * Terminal protocol node. Payload remains data-only until a registered renderer
 * interprets an allow-listed [type] after validation/normalization.
 */
@Serializable
data class ChildDataDto(
    val id: String,
    val type: String,
    val payload: JsonElement,
)
