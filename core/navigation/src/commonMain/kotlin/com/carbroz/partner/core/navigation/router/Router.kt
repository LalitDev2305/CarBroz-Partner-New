package com.carbroz.partner.core.navigation.router

import com.carbroz.partner.core.navigation.command.NavCommand
import com.carbroz.partner.core.navigation.result.NavResult
import com.carbroz.partner.core.navigation.state.NavState
import kotlinx.coroutines.flow.StateFlow

/**
 * Public thread-safe navigation controller interface.
 */
interface Router {
    val state: StateFlow<NavState>
    val currentState: NavState
    suspend fun execute(command: NavCommand): NavResult
}
