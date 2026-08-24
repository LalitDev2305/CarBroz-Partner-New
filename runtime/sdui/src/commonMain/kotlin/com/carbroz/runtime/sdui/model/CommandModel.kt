package com.carbroz.runtime.sdui.model

import kotlinx.serialization.json.JsonElement

/** Trusted semantic command marker. Concrete command families remain extensible. */
interface Command

data class RequestCommand(
    val method: RequestMethod,
    val endpoint: String,
    val destination: ScreenDestination,
    val payload: Map<String, JsonElement>,
) : Command

enum class RequestMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
}

data class ScreenDestination(
    val screenId: String,
    val templateId: String,
    val templateType: NodeType,
)
