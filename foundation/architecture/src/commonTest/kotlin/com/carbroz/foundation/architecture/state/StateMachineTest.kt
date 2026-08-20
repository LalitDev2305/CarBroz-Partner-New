package com.carbroz.foundation.architecture.state

import com.carbroz.foundation.architecture.reducer.Reducer
import kotlin.test.Test
import kotlin.test.assertEquals

class StateMachineTest {
    @Test
    fun `transition delegates deterministic result to reducer`() {
        val machine = StateMachine(Reducer<Int, Int> { previous, result -> previous + result })

        assertEquals(7, machine.transition(state = 3, result = 4))
        assertEquals(7, machine.transition(state = 3, result = 4))
    }

    @Test
    fun `transition preserves state when reducer defines no change`() {
        val machine = StateMachine(Reducer<String, Unit> { previous, _ -> previous })

        assertEquals("stable", machine.transition(state = "stable", result = Unit))
    }
}
