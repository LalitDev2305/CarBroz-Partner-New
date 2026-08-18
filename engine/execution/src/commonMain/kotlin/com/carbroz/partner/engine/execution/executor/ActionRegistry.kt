package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.domain.actions.model.ActionType

/**
 * Immutable lookup registry mapping ActionType to corresponding ActionExecutor implementations.
 */
class ActionRegistry private constructor(
    private val executors: Map<ActionType, ActionExecutor>
) {
    fun getExecutor(type: ActionType): ActionExecutor? = executors[type]

    companion object {
        val EMPTY = ActionRegistry(emptyMap())

        fun create(executors: Collection<ActionExecutor>): ActionRegistry {
            val map = mutableMapOf<ActionType, ActionExecutor>()
            for (executor in executors) {
                val type = executor.supportedType
                require(!map.containsKey(type)) {
                    "Duplicate ActionExecutor registration detected for ActionType: '${type.rawValue}'"
                }
                map[type] = executor
            }
            return ActionRegistry(map.toMap())
        }
    }
}
