package com.carbroz.partner.domain.actions.spec

import com.carbroz.partner.domain.actions.model.ActionId
import com.carbroz.partner.domain.actions.model.ActionType
import com.carbroz.partner.domain.actions.value.ActionParameters

/**
 * Pure declarative data contract representing ONE dynamic action specification.
 */
class ActionSpec private constructor(
    val id: ActionId,
    val type: ActionType,
    val parameters: ActionParameters,
    val metadata: ActionMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionSpec) return false
        return id == other.id &&
                type == other.type &&
                parameters == other.parameters &&
                metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "ActionSpec(id=${id.value}, type=${type.rawValue}, parameters=$parameters, metadata=$metadata)"
    }

    companion object {
        fun create(
            id: ActionId,
            type: ActionType,
            parameters: ActionParameters = ActionParameters.EMPTY,
            metadata: ActionMetadata = ActionMetadata.DEFAULT
        ): ActionSpec {
            return ActionSpec(id, type, parameters, metadata)
        }
    }
}
