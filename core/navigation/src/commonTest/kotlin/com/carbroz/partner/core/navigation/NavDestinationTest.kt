package com.carbroz.partner.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class NavDestinationTest {

    @Test
    fun verifyValidRouteAcceptedAndPreserved() {
        val dest = NavDestination.create("home/screen", mapOf("id" to "123"))
        assertEquals("home/screen", dest.route)
        assertEquals("123", dest.params["id"])
    }

    @Test
    fun verifyEmptyRouteRejected() {
        assertFailsWith<IllegalArgumentException> {
            NavDestination.create("")
        }
    }

    @Test
    fun verifyWhitespaceRouteRejected() {
        assertFailsWith<IllegalArgumentException> {
            NavDestination.create("   ")
        }
    }

    @Test
    fun verifyNavDestinationValueSemantics() {
        val dest1 = NavDestination.create("home", mapOf("id" to "123"))
        val dest2 = NavDestination.create("home", mapOf("id" to "123"))
        val dest3 = NavDestination.create("home", mapOf("id" to "456"))

        assertEquals(dest1, dest2)
        assertEquals(dest1.hashCode(), dest2.hashCode())
        assertNotEquals(dest1, dest3)
    }

    @Test
    fun verifyNavDestinationDefensiveCopying() {
        val mutableParams = mutableMapOf("key" to "value1")
        val dest = NavDestination.create("screen", mutableParams)

        mutableParams["key"] = "value2"

        assertEquals("value1", dest.params["key"])
    }

    @Test
    fun verifyToStringDoesNotExposeParameterValuesOrSecrets() {
        val sensitiveToken = "secret_auth_token_999"
        val dest = NavDestination.create("checkout", mapOf("token" to sensitiveToken, "amount" to "500"))

        val str = dest.toString()
        assertFalse(str.contains(sensitiveToken), "toString must not expose secret parameter values")
        assertFalse(str.contains("500"), "toString must not expose parameter values")
        assertEquals("NavDestination(route='checkout', paramCount=2)", str)
    }
}
