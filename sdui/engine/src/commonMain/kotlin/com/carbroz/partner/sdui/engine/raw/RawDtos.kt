package com.carbroz.partner.sdui.engine.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class RawScreenDto(
    @SerialName("schema_version") val schemaVersion: Int? = null,
    @SerialName("screen_id") val screenId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("show_back") val showBack: Boolean? = null,
    @SerialName("back") val back: RawBackDto? = null,
    @SerialName("refresh") val refresh: RawRefreshDto? = null,
    @SerialName("theme") val theme: RawThemeDto? = null,
    @SerialName("template") val template: RawTemplateDto? = null
)

@Serializable
public data class RawBackDto(
    @SerialName("api") val api: String? = null,
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("template_type") val templateType: String? = null
)

@Serializable
public data class RawRefreshDto(
    @SerialName("api") val api: String? = null
)

@Serializable
public data class RawThemeDto(
    @SerialName("theme_id") val themeId: String? = null,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
    @SerialName("gradient") val gradient: RawGradientDto? = null
)

@Serializable
public data class RawGradientDto(
    @SerialName("colors") val colors: List<String>? = null,
    @SerialName("angle") val angle: Float? = null
)

@Serializable
public data class RawTemplateDto(
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("template_type") val templateType: String? = null,
    @SerialName("width") val width: String? = null,
    @SerialName("height") val height: String? = null,
    @SerialName("axis") val axis: String? = null,
    @SerialName("alignment") val alignment: String? = null,
    @SerialName("arrangement") val arrangement: String? = null,
    @SerialName("padding") val padding: RawEdgeSpacingDto? = null,
    @SerialName("margin") val margin: RawEdgeSpacingDto? = null,
    @SerialName("gap") val gap: Int? = null,
    @SerialName("properties") val properties: JsonObject? = null,
    @SerialName("components") val components: List<RawComponentDto>? = null
)

@Serializable
public data class RawComponentDto(
    @SerialName("component_id") val componentId: String? = null,
    @SerialName("component_type") val componentType: String? = null,
    @SerialName("width") val width: String? = null,
    @SerialName("height") val height: String? = null,
    @SerialName("axis") val axis: String? = null,
    @SerialName("alignment") val alignment: String? = null,
    @SerialName("arrangement") val arrangement: String? = null,
    @SerialName("padding") val padding: RawEdgeSpacingDto? = null,
    @SerialName("margin") val margin: RawEdgeSpacingDto? = null,
    @SerialName("gap") val gap: Int? = null,
    @SerialName("visible") val visible: Boolean? = null,
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("properties") val properties: JsonObject? = null,
    @SerialName("action") val action: RawActionDto? = null,
    @SerialName("parent_action") val parentAction: RawParentActionDto? = null,
    @SerialName("subcomponents") val subcomponents: List<RawSubComponentDto>? = null,
    @SerialName("children_data") val childrenData: List<RawChildrenDataDto>? = null
)

@Serializable
public data class RawSubComponentDto(
    @SerialName("subcomponent_id") val subcomponentId: String? = null,
    @SerialName("subcomponent_type") val subcomponentType: String? = null,
    @SerialName("width") val width: String? = null,
    @SerialName("height") val height: String? = null,
    @SerialName("axis") val axis: String? = null,
    @SerialName("alignment") val alignment: String? = null,
    @SerialName("arrangement") val arrangement: String? = null,
    @SerialName("padding") val padding: RawEdgeSpacingDto? = null,
    @SerialName("margin") val margin: RawEdgeSpacingDto? = null,
    @SerialName("gap") val gap: Int? = null,
    @SerialName("visible") val visible: Boolean? = null,
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("properties") val properties: JsonObject? = null,
    @SerialName("action") val action: RawActionDto? = null,
    @SerialName("parent_action") val parentAction: RawParentActionDto? = null,
    @SerialName("children") val children: List<RawChildDto>? = null,
    @SerialName("children_data") val childrenData: List<RawChildrenDataDto>? = null
)

@Serializable
public data class RawChildDto(
    @SerialName("child_id") val childId: String? = null,
    @SerialName("child_type") val childType: String? = null,
    @SerialName("width") val width: String? = null,
    @SerialName("height") val height: String? = null,
    @SerialName("axis") val axis: String? = null,
    @SerialName("alignment") val alignment: String? = null,
    @SerialName("arrangement") val arrangement: String? = null,
    @SerialName("padding") val padding: RawEdgeSpacingDto? = null,
    @SerialName("margin") val margin: RawEdgeSpacingDto? = null,
    @SerialName("gap") val gap: Int? = null,
    @SerialName("visible") val visible: Boolean? = null,
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("properties") val properties: JsonObject? = null,
    @SerialName("action") val action: RawActionDto? = null,
    @SerialName("parent_action") val parentAction: RawParentActionDto? = null,
    @SerialName("children_data") val childrenData: List<RawChildrenDataDto>? = null
)

@Serializable
public data class RawChildrenDataDto(
    @SerialName("children_data_id") val childrenDataId: String? = null,
    @SerialName("children_data_type") val childrenDataType: String? = null,
    @SerialName("width") val width: String? = null,
    @SerialName("height") val height: String? = null,
    @SerialName("padding") val padding: RawEdgeSpacingDto? = null,
    @SerialName("margin") val margin: RawEdgeSpacingDto? = null,
    @SerialName("visible") val visible: Boolean? = null,
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("properties") val properties: JsonObject? = null,
    @SerialName("action") val action: RawActionDto? = null,
    @SerialName("parent_action") val parentAction: RawParentActionDto? = null
)

@Serializable
public data class RawEdgeSpacingDto(
    @SerialName("top") val top: Int? = null,
    @SerialName("bottom") val bottom: Int? = null,
    @SerialName("start") val start: Int? = null,
    @SerialName("end") val end: Int? = null
)

@Serializable
public data class RawActionDto(
    @SerialName("api") val api: String? = null,
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("template_type") val templateType: String? = null,
    @SerialName("payload") val payload: JsonObject? = null
)

@Serializable
public data class RawParentActionDto(
    @SerialName("target_id") val targetId: String? = null
)
