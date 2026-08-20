package com.carbroz.partner.engine.execution.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ActionSpecTest {

    @Test
    fun verifyActionSpecCreationAndValueSemantics() {
        val id = ActionId("act_1")
        val type = ActionType.API_REQUEST
        val params = ActionParameters.create(mapOf("endpoint" to ActionValue.Text("/v1/partner")))

        val spec1 = ActionSpec.create(id, type, params)
        val spec2 = ActionSpec.create(id, type, params)

        assertEquals(spec1, spec2)
        assertEquals(id, spec1.id)
        assertEquals(type, spec1.type)
        assertEquals(params, spec1.parameters)
    }

    @Test
    fun verifyActionSpecToStringDoesNotLeakPayloads() {
        val secretValue = "SUPER_SECRET_TOKEN"
        val params = ActionParameters.create(mapOf("token" to ActionValue.Text(secretValue)))
        val spec = ActionSpec.create(ActionId("act_1"), ActionType.API_REQUEST, params)

        val str = spec.toString()
        assertFalse(str.contains(secretValue))
    }

    @Test
    fun verifyAdversarialBackendActionRepresentationWithoutCodeChanges() {
        val actionTypesToTest = listOf(
            "api.request",
            "navigation.open",
            "navigation.back",
            "form.submit",
            "camera.capture",
            "location.current",
            "storage.write",
            "payment.start",
            "booking.accept",
            "booking.complete",
            "payment.upi_v2",
            "unknown.future.action"
        )

        actionTypesToTest.forEachIndexed { idx, rawType ->
            val type = ActionType(rawType)
            val spec = ActionSpec.create(
                id = ActionId("action_$idx"),
                type = type
            )
            assertEquals(rawType, spec.type.rawValue)
        }
    }
}
