package com.carbroz.sdui.value

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object JsonPathResolver {
    fun resolve(root: JsonElement?, path: String): JsonElement? {
        if (root == null) return null
        if (path.isBlank()) return root
        return path.split('.')
            .filter(String::isNotBlank)
            .fold(root as JsonElement?) { current, segment ->
                when (current) {
                    is JsonObject -> current[segment]
                    is JsonArray -> segment.toIntOrNull()?.let(current::getOrNull)
                    else -> null
                }
            }
    }
}
