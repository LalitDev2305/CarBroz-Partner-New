package com.carbroz.partner.domain.actions.spec

/**
 * Immutable metadata for action specification versioning and tracking.
 */
class ActionMetadata private constructor(
    val version: Int
) {
    init {
        require(version > 0) { "ActionMetadata version must be a positive integer" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionMetadata) return false
        return version == other.version
    }

    override fun hashCode(): Int = version.hashCode()
    override fun toString(): String = "ActionMetadata(version=$version)"

    companion object {
        val DEFAULT = ActionMetadata(version = 1)
        fun create(version: Int = 1): ActionMetadata = ActionMetadata(version)
    }
}
