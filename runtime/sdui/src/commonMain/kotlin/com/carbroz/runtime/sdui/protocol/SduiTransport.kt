package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Untrusted transport envelope. Never render or execute this model directly. */
@Serializable
data class SduiEnvelopeDto(
    val protocolVersion: Int,
    val schemaVersion: Int,
    val minimumClientVersion: Int = 1,
    val screen: ScreenDto,
    val requiredDefinitions: Set<String> = emptySet(),
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
    val properties: JsonObject = JsonObject(emptyMap()),
    val components: List<ComponentDto>,
)

@Serializable
data class ComponentDto(
    val id: String,
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val sections: List<SectionDto> = emptyList(),
    val elements: List<ElementDto> = emptyList(),
)

@Serializable
data class SectionDto(
    val id: String,
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val groups: List<GroupDto> = emptyList(),
    val elements: List<ElementDto> = emptyList(),
)

@Serializable
data class GroupDto(
    val id: String,
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val elements: List<ElementDto>,
)

/**
 * Terminal protocol node. Raw properties are decoded only by an allow-listed definition.
 * Interactive Elements may declare one semantic [command]; the Element definition owns
 * the native activation semantics, so the wire protocol does not repeat click/toggle triggers.
 */
@Serializable
data class ElementDto(
    val id: String,
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val command: CommandDto? = null,
)
