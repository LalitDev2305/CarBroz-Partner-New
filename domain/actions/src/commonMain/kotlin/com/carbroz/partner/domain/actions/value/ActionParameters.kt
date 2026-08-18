package com.carbroz.partner.domain.actions.value

/**
 * Immutable, defensively-copied container for dynamic action parameter maps.
 */
class ActionParameters private constructor(
    entries: Map<String, ActionValue>
) {
    val entries: Map<String, ActionValue> = entries.toMap()

    operator fun get(key: String): ActionValue? = entries[key]
    val size: Int get() = entries.size
    fun isEmpty(): Boolean = entries.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionParameters) return false
        return entries == other.entries
    }

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "ActionParameters(size=$size, keys=${entries.keys})"

    companion object {
        val EMPTY = ActionParameters(emptyMap())
        fun create(entries: Map<String, ActionValue> = emptyMap()): ActionParameters = ActionParameters(entries)
    }
}
