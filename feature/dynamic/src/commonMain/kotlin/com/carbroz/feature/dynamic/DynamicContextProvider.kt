package com.carbroz.feature.dynamic

import kotlin.random.Random
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Supplies the safe application/runtime values referenced by backend $context expressions. */
class DynamicContextProvider(
    private val flowContext: DynamicFlowContext,
    private val staticContext: JsonObject = JsonObject(emptyMap()),
    private val deviceId: String = createProcessDeviceId(),
) {
    suspend fun current(): JsonObject {
        val flow = flowContext.snapshot().context
        return deepMerge(
            JsonObject(
                buildMap {
                    putAll(staticContext)
                    put("deviceId", JsonPrimitive(deviceId))
                },
            ),
            flow,
        )
    }

    private fun deepMerge(base: JsonObject, update: JsonObject): JsonObject = JsonObject(
        buildMap {
            putAll(base)
            update.forEach { (key, value) ->
                val current = base[key]
                put(key, if (current is JsonObject && value is JsonObject) deepMerge(current, value) else value)
            }
        },
    )

    companion object {
        private fun createProcessDeviceId(): String = buildString {
            append("device-")
            repeat(4) {
                append(Random.nextLong().toULong().toString(16))
            }
        }
    }
}
