package com.carbroz.partner.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavStateTest {

    @Test
    fun verifyStateCannotBeEmpty() {
        assertFailsWith<IllegalArgumentException> {
            NavState.create(emptyList())
        }
    }

    @Test
    fun verifyNavStateActiveEntryIsLastAndSizeIsCorrect() {
        val entry1 = NavEntry("e1", NavDestination.create("root"))
        val entry2 = NavEntry("e2", NavDestination.create("details"))
        val state = NavState.create(listOf(entry1, entry2))

        assertEquals(2, state.size)
        assertEquals(entry2, state.activeEntry)
    }

    @Test
    fun verifyExternalListMutationCannotAffectNavState() {
        val entry1 = NavEntry("e1", NavDestination.create("root"))
        val entry2 = NavEntry("e2", NavDestination.create("details"))
        val mutableList = mutableListOf(entry1, entry2)

        val state = NavState.create(mutableList)
        mutableList.add(NavEntry("e3", NavDestination.create("settings")))

        assertEquals(2, state.size)
        assertEquals(entry2, state.activeEntry)
    }
}
