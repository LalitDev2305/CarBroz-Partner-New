package com.carbroz.partner.engine.execution.action

/**
 * Pure declarative data contract representing ONE dynamic action specification.
 */
public class ActionSpec private constructor(
    public val id: ActionId,
    public val type: ActionType,
    public val parameters: ActionParameters
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionSpec) return false
        return id == other.id &&
                type == other.type &&
                parameters == other.parameters
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    override fun toString(): String {
        return "ActionSpec(type=${type.rawValue}, parameterCount=${parameters.size})"
    }

    public companion object {
        public fun create(
            id: ActionId,
            type: ActionType,
            parameters: ActionParameters = ActionParameters.EMPTY
        ): ActionSpec {
            return ActionSpec(id, type, parameters)
        }
    }
}
