package com.carbroz.sdui.value

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface SduiValueResolution {
    data class Success(val value: JsonElement) : SduiValueResolution
    data class Failure(val reason: String) : SduiValueResolution
}

class SduiValueResolver {
    fun resolve(value: JsonElement, context: SduiExecutionContext): SduiValueResolution =
        when (value) {
            is JsonObject -> resolveObject(value, context)
            is JsonArray -> resolveArray(value, context)
            else -> SduiValueResolution.Success(value)
        }

    private fun resolveObject(value: JsonObject, context: SduiExecutionContext): SduiValueResolution {
        reference(value, context)?.let { return it }
        val resolved = linkedMapOf<String, JsonElement>()
        value.forEach { (key, child) ->
            when (val result = resolve(child, context)) {
                is SduiValueResolution.Success -> resolved[key] = result.value
                is SduiValueResolution.Failure -> return result
            }
        }
        return SduiValueResolution.Success(JsonObject(resolved))
    }

    private fun resolveArray(value: JsonArray, context: SduiExecutionContext): SduiValueResolution {
        val resolved = ArrayList<JsonElement>(value.size)
        value.forEach { child ->
            when (val result = resolve(child, context)) {
                is SduiValueResolution.Success -> resolved += result.value
                is SduiValueResolution.Failure -> return result
            }
        }
        return SduiValueResolution.Success(JsonArray(resolved))
    }

    private fun reference(value: JsonObject, context: SduiExecutionContext): SduiValueResolution? {
        if (value.size != 1) return null
        value["\$binding"]?.let { raw ->
            val key = (raw as? JsonPrimitive)?.content ?: return SduiValueResolution.Failure("binding_reference_invalid")
            return context.bindings[key]
                ?.let(SduiValueResolution::Success)
                ?: SduiValueResolution.Failure("binding_missing:$key")
        }
        value["\$context"]?.let { raw ->
            val path = (raw as? JsonPrimitive)?.content ?: return SduiValueResolution.Failure("context_reference_invalid")
            return JsonPathResolver.resolve(context.context, path)
                ?.let(SduiValueResolution::Success)
                ?: SduiValueResolution.Failure("context_missing:$path")
        }
        value["\$response"]?.let { raw ->
            val path = (raw as? JsonPrimitive)?.content ?: return SduiValueResolution.Failure("response_reference_invalid")
            return JsonPathResolver.resolve(context.response, path)
                ?.let(SduiValueResolution::Success)
                ?: SduiValueResolution.Failure("response_missing:$path")
        }
        if (value.containsKey("\$literal")) {
            return SduiValueResolution.Success(value["\$literal"] ?: JsonNull)
        }
        return null
    }
}
