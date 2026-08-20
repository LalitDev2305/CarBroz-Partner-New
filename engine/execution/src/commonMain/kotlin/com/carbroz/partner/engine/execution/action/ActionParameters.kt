package com.carbroz.partner.engine.execution.action

/**
 * Immutable, defensively-copied container for dynamic action parameter maps.
 */
public class ActionParameters private constructor(
    entries: Map<String, ActionValue>
) {
    public val entries: Map<String, ActionValue> = entries.toMap()

    init {
        require(entries.keys.all { it.isNotBlank() }) { "ActionParameters keys must not be blank" }
    }

    public operator fun get(key: String): ActionValue? = entries[key]
    public val size: Int get() = entries.size
    public fun isEmpty(): Boolean = entries.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionParameters) return false
        return entries == other.entries
    }

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "ActionParameters(size=$size, keys=${entries.keys})"

    public companion object {
        public val EMPTY: ActionParameters = ActionParameters(emptyMap())
        public fun create(entries: Map<String, ActionValue> = emptyMap()): ActionParameters = ActionParameters(entries)
    }
}
