package com.carbroz.partner.domain.storage.preference

/**
 * Type-safe key definitions for domain preference storage.
 */
sealed interface PreferenceKey<T> {
    val name: String
    val defaultValue: T

    data class StringKey(
        override val name: String,
        override val defaultValue: String = ""
    ) : PreferenceKey<String> {
        init {
            require(name.isNotBlank()) { "PreferenceKey name must not be blank" }
        }
    }

    data class BooleanKey(
        override val name: String,
        override val defaultValue: Boolean = false
    ) : PreferenceKey<Boolean> {
        init {
            require(name.isNotBlank()) { "PreferenceKey name must not be blank" }
        }
    }

    data class IntKey(
        override val name: String,
        override val defaultValue: Int = 0
    ) : PreferenceKey<Int> {
        init {
            require(name.isNotBlank()) { "PreferenceKey name must not be blank" }
        }
    }

    data class LongKey(
        override val name: String,
        override val defaultValue: Long = 0L
    ) : PreferenceKey<Long> {
        init {
            require(name.isNotBlank()) { "PreferenceKey name must not be blank" }
        }
    }
}
