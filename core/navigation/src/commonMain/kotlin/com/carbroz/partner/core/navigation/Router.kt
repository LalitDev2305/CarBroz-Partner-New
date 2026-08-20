package com.carbroz.partner.core.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * Public thread-safe navigation controller interface.
 */
interface Router {
    val state: StateFlow<NavState>
    suspend fun execute(command: NavCommand): NavResult
}
