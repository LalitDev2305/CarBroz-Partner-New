package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface CommandDto

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

@Serializable
@SerialName("CAPABILITY")
data class CapabilityCommandDto(
    val capability: String,
    val operation: String,
    val arguments: JsonObject = JsonObject(emptyMap()),
) : CommandDto

@Serializable
@SerialName("NAVIGATION")
data class NavigationCommandDto(
    val operation: String,
    val targetNavigationId: String? = null,
) : CommandDto

@Serializable
@SerialName("PRESENTATION")
data class PresentationCommandDto(
    val operation: String,
    val presentationKind: String,
    val id: String,
    val properties: JsonObject = JsonObject(emptyMap()),
) : CommandDto

@Serializable
@SerialName("LOCAL_STATE")
data class LocalStateCommandDto(val values: JsonObject) : CommandDto

@Serializable
@SerialName("FORM")
data class FormCommandDto(val operation: String) : CommandDto

@Serializable
@SerialName("BACKGROUND")
data class BackgroundCommandDto(
    val operation: String,
    val id: String,
    val workKind: String = "PROCESSING",
    val earliestStartDelayMillis: Long = 0L,
    val networkRequirement: String = "NOT_REQUIRED",
    val requiresCharging: Boolean = false,
    val title: String? = null,
    val description: String? = null,
    val input: JsonObject = JsonObject(emptyMap()),
) : CommandDto
