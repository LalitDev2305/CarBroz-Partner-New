package com.carbroz.foundation.security

/**
 * In-memory wrapper for sensitive text that deliberately redacts [toString].
 *
 * This does not provide memory encryption. Its purpose is to reduce accidental
 * disclosure through logs, debugging, string interpolation, and diagnostics.
 */
class Secret private constructor(
    private val rawValue: String,
) {
    fun reveal(): String = rawValue

    override fun toString(): String = "Secret(**redacted**)"

    override fun equals(other: Any?): Boolean =
        other is Secret && rawValue == other.rawValue

    override fun hashCode(): Int = rawValue.hashCode()

    companion object {
        fun of(value: String): Secret {
            require(value.isNotBlank()) { "Secret value must not be blank." }
            return Secret(value)
        }
    }
}
