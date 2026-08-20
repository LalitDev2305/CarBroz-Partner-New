package com.carbroz.partner.core.navigation

/**
 * Immutable state snapshot container exposing active navigation stack entries.
 *
 * Guarantees that [entries] contains at least one element, [activeEntry] is the top entry, and external callers cannot mutate internal state.
 */
class NavState internal constructor(
    entries: List<NavEntry>
) {
    val entries: List<NavEntry> = entries.toList()

    val activeEntry: NavEntry
        get() = entries.last()

    val size: Int
        get() = entries.size

    init {
        require(entries.isNotEmpty()) { "NavState entries must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavState) return false
        return entries == other.entries
    }

    override fun hashCode(): Int {
        return entries.hashCode()
    }

    override fun toString(): String {
        return "NavState(size=$size, activeEntry=${activeEntry.destination.route})"
    }

    companion object {
        internal fun create(entries: List<NavEntry>): NavState {
            return NavState(entries)
        }

        internal fun create(rootEntry: NavEntry): NavState {
            return NavState(listOf(rootEntry))
        }
    }
}
