package com.carbroz.foundation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

/**
 * Navigation 3 key owned strictly by the adapter boundary.
 *
 * Semantic destinations remain framework-independent and are translated to
 * this key only for presentation. The semantic navigationId remains the
 * canonical application identity.
 */
internal data class Navigation3Key(
    val navigationId: String,
) : NavKey

/**
 * Resolves semantic destinations into Compose content without exposing
 * Navigation 3 framework classes to feature/runtime/domain callers.
 */
fun interface NavigationDestinationContent {
    @Composable
    fun Content(destination: NavigationDestination)
}

/**
 * Presentation adapter over the application-owned semantic back stack.
 *
 * Navigation 3 receives a projection of [NavigationState]; it does not own a
 * second CarBroz stack. Back requests are emitted as semantic commands and must
 * be reduced by the canonical [NavigationReducer].
 */
@Composable
fun Navigation3Host(
    state: NavigationState,
    destinationContent: NavigationDestinationContent,
    onCommand: (NavigationCommand) -> Unit,
) {
    val destinationsById = state.backStack.associateBy { it.navigationId }
    val keys = state.backStack.map { Navigation3Key(it.navigationId) }

    NavDisplay(
        backStack = keys,
        onBack = { onCommand(NavigationCommand.Pop) },
        entryProvider = { key ->
            val destination = destinationsById.getValue(key.navigationId)
            NavEntry(key) {
                destinationContent.Content(destination)
            }
        },
    )
}
