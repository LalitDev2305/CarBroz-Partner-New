package com.carbroz.partner.core.navigation.destination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NavDestinationTest {

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
}
