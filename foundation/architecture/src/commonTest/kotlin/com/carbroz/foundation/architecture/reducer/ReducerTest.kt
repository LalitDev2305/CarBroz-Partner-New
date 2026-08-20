package com.carbroz.foundation.architecture.reducer

import kotlin.test.Test
import kotlin.test.assertEquals

class ReducerTest {
    @Test
    fun reducer_returns_expected_next_state() {
        val reducer = Reducer<Int, Int> { previous, result -> previous + result }

        assertEquals(5, reducer.reduce(2, 3))
    }
}
