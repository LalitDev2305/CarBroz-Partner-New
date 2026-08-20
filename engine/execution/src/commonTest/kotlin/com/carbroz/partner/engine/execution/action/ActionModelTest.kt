package com.carbroz.partner.engine.execution.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ActionModelTest {

    @Test
    fun verifyActionIdValidAndValueSemantics() {
        val id1 = ActionId("action_123")
        val id2 = ActionId("action_123")
        val id3 = ActionId("action_456")

        assertEquals("action_123", id1.value)
        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
    }

    @Test
    fun verifyActionIdBlankRejection() {
        assertFailsWith<IllegalArgumentException> {
            ActionId("")
        }
        assertFailsWith<IllegalArgumentException> {
            ActionId("   ")
        }
    }

    @Test
    fun verifyActionTypeKnownAndUnknownExtensibility() {
        val apiType = ActionType.API_REQUEST
        val logoutType = ActionType.AUTH_LOGOUT
        val customBackendType = ActionType("payment.upi_v2")

        assertEquals("api.request", apiType.rawValue)
        assertEquals("auth.logout", logoutType.rawValue)
        assertEquals("payment.upi_v2", customBackendType.rawValue)
        assertEquals(ActionType("payment.upi_v2"), customBackendType)
    }

    @Test
    fun verifyActionTypeBlankRejection() {
        assertFailsWith<IllegalArgumentException> {
            ActionType("")
        }
        assertFailsWith<IllegalArgumentException> {
            ActionType("   ")
        }
    }
}
