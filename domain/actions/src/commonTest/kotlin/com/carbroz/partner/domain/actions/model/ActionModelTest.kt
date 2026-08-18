package com.carbroz.partner.domain.actions.model

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
        val pushType = ActionType.NAVIGATION_PUSH
        val customBackendType = ActionType("payment.upi_v2")

        assertEquals("navigation.push", pushType.rawValue)
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
