package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Untrusted command instruction attached to an interactive Element.
 *
 * Wire contracts intentionally expose only semantic server intent. Runtime dispatch,
 * binding resolution, networking, navigation and MVI orchestration stay client-owned.
 */
@Serializable
sealed interface CommandDto

/**
 * Acquires the next SDUI screen from a trusted endpoint.
 *
 * Destination metadata is declared before execution so compatibility can be checked
 * before the request and the returned screen can be verified against the requested
 * destination contract. [templateType] selects the registered template definition;
 * [screenId] and [templateId] are destination identity, not hard-coded screen routing.
 */
@Serializable
@SerialName("REQUEST")
data class RequestCommandDto(
    val method: String,
    val endpoint: String,
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val payload: JsonObject = JsonObject(emptyMap()),
) : CommandDto
