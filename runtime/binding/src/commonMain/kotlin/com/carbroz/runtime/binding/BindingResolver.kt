package com.carbroz.runtime.binding

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Supplies trusted runtime values for one semantic namespace. */
fun interface BindingValueSource {
    fun resolve(path: List<String>): JsonElement?
}

data class BindingContext(
    private val sources: Map<BindingNamespace, BindingValueSource>,
) {
    fun source(namespace: BindingNamespace): BindingValueSource? = sources[namespace]

    companion object {
        fun of(vararg sources: Pair<BindingNamespace, BindingValueSource>): BindingContext =
            BindingContext(sources.toMap())
    }
}

sealed interface BindingResolutionResult {
    data class Success(val value: JsonElement) : BindingResolutionResult

    data class Failure(
        val error: BindingResolutionError,
    ) : BindingResolutionResult
}

sealed interface BindingResolutionError {
    data class MissingSource(val namespace: BindingNamespace) : BindingResolutionError
    data class MissingValue(val reference: BindingReference) : BindingResolutionError
}

/**
 * Resolves exact binding references recursively while preserving JSON primitive/object/array types.
 * Literal strings remain literal unless the entire string is a valid binding reference.
 */
class BindingResolver {
    fun resolve(value: JsonElement, context: BindingContext): BindingResolutionResult = when (value) {
        is JsonObject -> resolveObject(value, context)
        is JsonArray -> resolveArray(value, context)
        is JsonPrimitive -> resolvePrimitive(value, context)
    }

    private fun resolveObject(value: JsonObject, context: BindingContext): BindingResolutionResult {
        val resolved = linkedMapOf<String, JsonElement>()
        for ((key, child) in value) {
            when (val result = resolve(child, context)) {
                is BindingResolutionResult.Success -> resolved[key] = result.value
                is BindingResolutionResult.Failure -> return result
            }
        }
        return BindingResolutionResult.Success(JsonObject(resolved))
    }

    private fun resolveArray(value: JsonArray, context: BindingContext): BindingResolutionResult {
        val resolved = ArrayList<JsonElement>(value.size)
        for (child in value) {
            when (val result = resolve(child, context)) {
                is BindingResolutionResult.Success -> resolved += result.value
                is BindingResolutionResult.Failure -> return result
            }
        }
        return BindingResolutionResult.Success(JsonArray(resolved))
    }

    private fun resolvePrimitive(value: JsonPrimitive, context: BindingContext): BindingResolutionResult {
        if (!value.isString) return BindingResolutionResult.Success(value)
        val reference = BindingReference.parse(value.content) ?: return BindingResolutionResult.Success(value)
        val source = context.source(reference.namespace)
            ?: return BindingResolutionResult.Failure(BindingResolutionError.MissingSource(reference.namespace))
        val resolved = source.resolve(reference.path)
            ?: return BindingResolutionResult.Failure(BindingResolutionError.MissingValue(reference))
        return BindingResolutionResult.Success(resolved)
    }
}
