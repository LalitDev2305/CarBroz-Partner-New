package com.carbroz.foundation.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultAppLifecycleTest {
    @Test
    fun `starts unknown by default`() {
        val lifecycle = DefaultAppLifecycle()

        assertEquals(AppLifecycleState.Unknown, lifecycle.state.value)
    }

    @Test
    fun `moves between foreground and background`() {
        val lifecycle = DefaultAppLifecycle()

        lifecycle.moveTo(AppLifecycleState.Foreground)
        assertEquals(AppLifecycleState.Foreground, lifecycle.state.value)

        lifecycle.moveTo(AppLifecycleState.Background)
        assertEquals(AppLifecycleState.Background, lifecycle.state.value)
    }

    @Test
    fun `duplicate transition remains stable`() {
        val lifecycle = DefaultAppLifecycle(AppLifecycleState.Foreground)

        lifecycle.moveTo(AppLifecycleState.Foreground)

        assertEquals(AppLifecycleState.Foreground, lifecycle.state.value)
    }
}
