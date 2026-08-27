package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Untrusted semantic command instruction attached to an interactive Element. */
@Serializable
sealed interface CommandDto

/**
 * Trusted request intent is normalized from this wire DTO. A request may either return another
 * dynamic screen or complete without a screen transition. Business screen names never appear here.
 */
@Serializable
@SerialName("REQUEST")
data class RequestCommandDto(
    val method: String,
    val endpoint: String,
    val screenId: String? = null,
    val templateId: String? = null,
    val templateType: String? = null,
    val payload: JsonObject = JsonObject(emptyMap()),
    val authentication: String = "SESSION",
    val responseMode: String = "SCREEN",
    val transition: String = "PUSH",
    val backStackKey: String? = null,
    val validateForm: Boolean = true,
) : CommandDto

/** Semantic platform capability request. Platform APIs and provider selection stay client-owned. */
@Serializable
@SerialName("CAPABILITY")
data class CapabilityCommandDto(
    val capability: String,
    val operation: String,
    val arguments: JsonObject = JsonObject(emptyMap()),
) : CommandDto

/** Back-stack operation that requires no network request. */
@Serializable
@SerialName("NAVIGATION")
data class NavigationCommandDto(
    val operation: String,
    val targetNavigationId: String? = null,
) : CommandDto

/** Generic presentation effect; application composition decides how it is displayed. */
@Serializable
@SerialName("PRESENTATION")
data class PresentationCommandDto(
    val operation: String,
    val presentationKind: String,
    val id: String,
    val properties: JsonObject = JsonObject(emptyMap()),
) : CommandDto

/** Screen-runtime local state mutation. */
@Serializable
@SerialName("LOCAL_STATE")
data class LocalStateCommandDto(
    val values: JsonObject,
) : CommandDto

/** Generic form operation owned by runtime:form. */
@Serializable
@SerialName("FORM")
data class FormCommandDto(
    val operation: String,
) : CommandDto
