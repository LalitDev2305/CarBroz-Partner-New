package com.carbroz.partner.core.navigation.stack

/**
 * Functional interface for generating unique navigation entry identifiers.
 */
fun interface NavEntryIdGenerator {
    fun generateId(): String
}

/**
 * Standard thread-safe monotonic counter generator for navigation entry IDs.
 */
class DefaultNavEntryIdGenerator(
    private val prefix: String = "entry_"
) : NavEntryIdGenerator {
    private var counter: Long = 0L

    override fun generateId(): String {
        return "$prefix${++counter}"
    }
}

