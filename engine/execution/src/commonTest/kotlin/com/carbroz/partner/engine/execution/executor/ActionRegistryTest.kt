package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.result.ExecutionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ActionRegistryTest {

    private class TestExecutor(override val supportedType: ActionType) : ActionExecutor {
        override suspend fun execute(action: ActionSpec): ExecutionResult = ExecutionResult.Success()
    }

    @Test
    fun verifyEmptyRegistryReturnsNull() {
        val registry = ActionRegistry.EMPTY
        assertNull(registry.getExecutor(ActionType("test.action")))
    }

    @Test
    fun verifySuccessfulExecutorRegistrationAndLookup() {
        val type1 = ActionType("nav.push")
        val type2 = ActionType("network.post")
        val exec1 = TestExecutor(type1)
        val exec2 = TestExecutor(type2)

        val registry = ActionRegistry.create(listOf(exec1, exec2))

        assertEquals(exec1, registry.getExecutor(type1))
        assertEquals(exec2, registry.getExecutor(type2))
        assertNull(registry.getExecutor(ActionType("other")))
    }

    @Test
    fun verifyDuplicateActionTypeClaimRejected() {
        val type = ActionType("duplicate.type")
        val exec1 = TestExecutor(type)
        val exec2 = TestExecutor(type)

        assertFailsWith<IllegalArgumentException> {
            ActionRegistry.create(listOf(exec1, exec2))
        }
    }

    @Test
    fun verifyDefensiveCopyImmutability() {
        val type = ActionType("test.type")
        val exec = TestExecutor(type)
        val list = mutableListOf<ActionExecutor>(exec)

        val registry = ActionRegistry.create(list)
        list.clear()

        assertEquals(exec, registry.getExecutor(type))
    }
}
