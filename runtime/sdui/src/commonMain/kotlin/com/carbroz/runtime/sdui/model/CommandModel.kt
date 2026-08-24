package com.carbroz.runtime.sdui.model

import kotlinx.serialization.json.JsonElement

@kotlin.jvm.JvmInline
value class CommandKind(val value: String)

/** Trusted semantic command marker. Concrete command families remain extensible. */
interface Command {
    val kind: CommandKind
}

data class RequestCommand(
    val method: RequestMethod,
    val endpoint: String,
    val destination: ScreenDestination,
    val payload: Map<String, JsonElement>,
) : Command {
    override val kind: CommandKind = KIND

    companion object {
        val KIND: CommandKind = CommandKind("REQUEST")
    }
}

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
