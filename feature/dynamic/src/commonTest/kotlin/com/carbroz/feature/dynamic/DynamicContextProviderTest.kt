package com.carbroz.feature.dynamic

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicContextProviderTest {
    @Test
    fun currentCombinesStaticDeviceAndTransientFlowContextWithoutOwningState() = runTest {
        val flow = DynamicFlowContext()
        flow.updateContext(
            JsonObject(
                mapOf(
                    "authFlow" to JsonObject(mapOf("phone" to JsonPrimitive("9999999999"))),
                    "nested" to JsonObject(mapOf("flow" to JsonPrimitive("value"))),
                ),
            ),
        )
        val provider = DynamicContextProvider(
            flowContext = flow,
            staticContext = JsonObject(
                mapOf(
                    "legal" to JsonObject(mapOf("termsUri" to JsonPrimitive("https://example.com/terms"))),
                    "nested" to JsonObject(mapOf("static" to JsonPrimitive("value"))),
                ),
            ),
            deviceId = "device-test",
        )

        val current = provider.current()

        assertEquals(JsonPrimitive("device-test"), current["deviceId"])
        assertEquals(JsonPrimitive("https://example.com/terms"), current["legal"]!!.jsonObject["termsUri"])
        assertEquals(JsonPrimitive("9999999999"), current["authFlow"]!!.jsonObject["phone"])
        assertEquals(JsonPrimitive("value"), current["nested"]!!.jsonObject["static"])
        assertEquals(JsonPrimitive("value"), current["nested"]!!.jsonObject["flow"])
    }
}
