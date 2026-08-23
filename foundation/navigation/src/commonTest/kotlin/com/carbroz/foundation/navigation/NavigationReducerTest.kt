package com.carbroz.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NavigationReducerTest {
    @Test
    fun emptyBackStackIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            NavigationState(emptyList())
        }
    }

    @Test
    fun pushAppendsDestination() {
        val root = destination("root")
        val detail = destination("detail")

        val result = NavigationReducer.reduce(
            NavigationState(listOf(root)),
            NavigationCommand.Push(detail),
        )

        assertEquals(listOf(root, detail), assertIs<NavigationTransition.Applied>(result).state.backStack)
    }

    @Test
    fun popNeverRemovesRoot() {
        val root = destination("root")
        val state = NavigationState(listOf(root))

        val result = NavigationReducer.reduce(state, NavigationCommand.Pop)

        val ignored = assertIs<NavigationTransition.Ignored>(result)
        assertEquals(NavigationIgnoreReason.AlreadyAtRoot, ignored.reason)
        assertEquals(state, ignored.state)
    }

    @Test
    fun replaceTopPreservesEarlierEntries() {
        val root = destination("root")
        val first = destination("first")
        val replacement = destination("replacement")

        val result = NavigationReducer.reduce(
            NavigationState(listOf(root, first)),
            NavigationCommand.ReplaceTop(replacement),
        )

        assertEquals(
            listOf(root, replacement),
            assertIs<NavigationTransition.Applied>(result).state.backStack,
        )
    }

    @Test
    fun resetCreatesNewRoot() {
        val nextRoot = destination("authenticated-root")

        val result = NavigationReducer.reduce(
            NavigationState(listOf(destination("splash"), destination("old"))),
            NavigationCommand.ResetTo(nextRoot),
        )

        assertEquals(
            listOf(nextRoot),
            assertIs<NavigationTransition.Applied>(result).state.backStack,
        )
    }

    @Test
    fun popToUsesLastMatchingSemanticDestination() {
        val first = destination("repeated")
        val middle = destination("middle")
        val second = destination("repeated")
        val leaf = destination("leaf")

        val result = NavigationReducer.reduce(
            NavigationState(listOf(first, middle, second, leaf)),
            NavigationCommand.PopTo("repeated"),
        )

        assertEquals(
            listOf(first, middle, second),
            assertIs<NavigationTransition.Applied>(result).state.backStack,
        )
    }

    @Test
    fun popToUnknownDestinationIsIgnored() {
        val state = NavigationState(listOf(destination("root"), destination("detail")))

        val result = NavigationReducer.reduce(state, NavigationCommand.PopTo("missing"))

        val ignored = assertIs<NavigationTransition.Ignored>(result)
        assertEquals(NavigationIgnoreReason.DestinationNotFound, ignored.reason)
        assertEquals(state, ignored.state)
    }

    @Test
    fun inclusivePopCannotRemoveRoot() {
        val root = destination("root")
        val state = NavigationState(listOf(root, destination("detail")))

        val result = NavigationReducer.reduce(
            state,
            NavigationCommand.PopTo(root.navigationId, inclusive = true),
        )

        val ignored = assertIs<NavigationTransition.Ignored>(result)
        assertEquals(NavigationIgnoreReason.AlreadyAtRoot, ignored.reason)
        assertEquals(state, ignored.state)
    }

    private fun destination(id: String): NavigationDestination = TestDestination(id)

    private data class TestDestination(
        override val navigationId: String,
    ) : NavigationDestination
}
