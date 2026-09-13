package com.carbroz.feature.dynamic

import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiDestination
import com.carbroz.sdui.model.SduiRequestMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DynamicDestinationFlowContextTest {
    @Test
    fun destination_roundTripsEveryBackendLoadField() {
        val original = destination()
        val encoded = Json.encodeToString(original)
        val restored = Json.decodeFromString<DynamicDestination>(encoded)

        assertEquals(original, restored)
        assertEquals("dynamic:booking_details:details_template:/api/v1/screens/booking-details", restored.navigationId)
    }

    @Test
    fun sduiDestinationConversion_isLossless() {
        val original = SduiDestination(
            screenId = "dashboard",
            templateId = "dashboard_template",
            templateType = "stack_template",
            endpoint = "/api/v1/screens/dashboard",
            method = SduiRequestMethod.GET,
            authentication = SduiAuthentication.SESSION,
        )

        val dynamic = DynamicDestination.from(original)

        assertEquals(original, dynamic.toSduiDestination())
    }

    @Test
    fun destinationRejectsUnsafeEndpointAndNonGetLoadMethod() {
        assertFailsWith<IllegalArgumentException> {
            destination(endpoint = "https://example.com/screen")
        }
        assertFailsWith<IllegalArgumentException> {
            destination(method = SduiRequestMethod.POST)
        }
    }

    @Test
    fun flowContextDeepMergesContextAndReplacesLatestResponse() = runTest {
        val flow = DynamicFlowContext()
        flow.updateContext(
            JsonObject(
                mapOf(
                    "auth" to JsonObject(mapOf("phone" to JsonPrimitive("9999999999"))),
                    "stable" to JsonPrimitive("keep"),
                ),
            ),
        )
        flow.updateContext(
            JsonObject(
                mapOf(
                    "auth" to JsonObject(mapOf("challengeId" to JsonPrimitive("challenge-2"))),
                ),
            ),
        )
        flow.updateResponse(JsonObject(mapOf("request" to JsonPrimitive(1))))
        flow.updateResponse(JsonObject(mapOf("request" to JsonPrimitive(2))))

        val snapshot = flow.snapshot()
        val auth = snapshot.context["auth"]!!.jsonObject

        assertEquals(JsonPrimitive("9999999999"), auth["phone"])
        assertEquals(JsonPrimitive("challenge-2"), auth["challengeId"])
        assertEquals(JsonPrimitive("keep"), snapshot.context["stable"])
        assertEquals(JsonPrimitive(2), snapshot.response!!.jsonObject["request"])
    }

    @Test
    fun clearRemovesTransientContextAndResponse() = runTest {
        val flow = DynamicFlowContext()
        flow.updateContext(JsonObject(mapOf("key" to JsonPrimitive("value"))))
        flow.updateResponse(JsonObject(mapOf("ok" to JsonPrimitive(true))))

        flow.clear()

        val snapshot = flow.snapshot()
        assertEquals(JsonObject(emptyMap()), snapshot.context)
        assertNull(snapshot.response)
    }

    private fun destination(
        endpoint: String = "/api/v1/screens/booking-details",
        method: SduiRequestMethod = SduiRequestMethod.GET,
    ): DynamicDestination = DynamicDestination(
        screenId = "booking_details",
        templateId = "details_template",
        templateType = "stack_template",
        endpoint = endpoint,
        method = method,
        authentication = SduiAuthentication.SESSION,
    )
}
