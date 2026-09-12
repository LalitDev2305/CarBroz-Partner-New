package com.carbroz.feature.dynamic

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Process-scoped transient execution context shared only between dynamic screens in the same flow.
 * It is not UI state and is never persisted. This is where request-declared contextUpdates and the
 * latest successful action response live so a later screen can resolve $context / $response.
 */
class DynamicFlowContext {
    private val mutex = Mutex()
    private var context: JsonObject = JsonObject(emptyMap())
    private var response: JsonElement? = null

    suspend fun snapshot(): DynamicFlowSnapshot = mutex.withLock {
        DynamicFlowSnapshot(context = context, response = response)
    }

    suspend fun updateContext(values: JsonObject) = mutex.withLock {
        context = deepMerge(context, values)
    }

    suspend fun updateResponse(value: JsonElement?) = mutex.withLock {
        response = value
    }

    suspend fun commitSuccessfulRequest(
        responseValue: JsonElement?,
        contextUpdates: JsonObject?,
    ) = mutex.withLock {
        response = responseValue
        if (contextUpdates != null) context = deepMerge(context, contextUpdates)
    }

    suspend fun clear() = mutex.withLock {
        context = JsonObject(emptyMap())
        response = null
    }

    private fun deepMerge(base: JsonObject, update: JsonObject): JsonObject = JsonObject(
        buildMap {
            putAll(base)
            update.forEach { (key, value) ->
                val current = base[key]
                put(
                    key,
                    if (current is JsonObject && value is JsonObject) deepMerge(current, value) else value,
                )
            }
        },
    )
}

data class DynamicFlowSnapshot(
    val context: JsonObject,
    val response: JsonElement?,
)
