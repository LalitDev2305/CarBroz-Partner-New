package com.carbroz.partner.core.navigation.stack

import com.carbroz.partner.core.navigation.destination.NavDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavStackTest {

    @Test
    fun verifyNavStackNonEmptyInvariant() {
        assertFailsWith<IllegalArgumentException> {
            NavStack.create(emptyList())
        }
    }

    @Test
    fun verifyNavStackDefensiveCopying() {
        val entry1 = NavEntry("1", NavDestination.create("root"))
        val entry2 = NavEntry("2", NavDestination.create("home"))
        val mutableList = mutableListOf(entry1, entry2)

        val stack = NavStack.create(mutableList)
        mutableList.clear()

        assertEquals(2, stack.size)
        assertEquals(entry2, stack.current)
    }
}
