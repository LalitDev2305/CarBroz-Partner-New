package com.carbroz.partner.core.navigation.stack

/**
 * Immutable, non-empty back stack data container.
 *
 * Guarantees that [entries] contains at least one element.
 */
class NavStack private constructor(
    entries: List<NavEntry>
) {
    val entries: List<NavEntry> = entries.toList()

    val current: NavEntry
        get() = entries.last()

    val size: Int
        get() = entries.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavStack) return false
        return entries == other.entries
    }

    override fun hashCode(): Int {
        return entries.hashCode()
    }

    override fun toString(): String {
        return "NavStack(size=$size, current=${current.destination.route})"
    }

    companion object {
        fun create(entries: List<NavEntry>): NavStack {
            require(entries.isNotEmpty()) { "NavStack entries must not be empty" }
            return NavStack(entries)
        }

        fun create(rootEntry: NavEntry): NavStack {
            return NavStack(listOf(rootEntry))
        }
    }
}
