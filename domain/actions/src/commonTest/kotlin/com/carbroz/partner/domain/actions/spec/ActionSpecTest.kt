package com.carbroz.partner.domain.actions.spec

import com.carbroz.partner.domain.actions.model.ActionId
import com.carbroz.partner.domain.actions.model.ActionType
import com.carbroz.partner.domain.actions.value.ActionParameters
import com.carbroz.partner.domain.actions.value.ActionValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActionSpecTest {

    @Test
    fun verifyActionMetadataDefaultAndValidation() {
        val defaultMeta = ActionMetadata.DEFAULT
        assertEquals(1, defaultMeta.version)

        val customMeta = ActionMetadata.create(version = 2)
        assertEquals(2, customMeta.version)

        assertFailsWith<IllegalArgumentException> {
            ActionMetadata.create(version = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            ActionMetadata.create(version = -1)
        }
    }

    @Test
    fun verifyActionSpecCreationAndValueSemantics() {
        val id = ActionId("act_1")
        val type = ActionType.API_REQUEST
        val params = ActionParameters.create(mapOf("endpoint" to ActionValue.Text("/v1/partner")))
        val meta = ActionMetadata.create(1)

        val spec1 = ActionSpec.create(id, type, params, meta)
        val spec2 = ActionSpec.create(id, type, params, meta)

        assertEquals(spec1, spec2)
        assertEquals(id, spec1.id)
        assertEquals(type, spec1.type)
        assertEquals(params, spec1.parameters)
        assertEquals(meta, spec1.metadata)
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
