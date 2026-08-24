package com.carbroz.data.preferences

import kotlinx.coroutines.flow.Flow

/** Strongly typed identifier for a non-sensitive preference value. */
sealed class PreferenceKey<T> protected constructor(val name: String) {
    class StringKey(name: String) : PreferenceKey<String>(validated(name))
    class BooleanKey(name: String) : PreferenceKey<Boolean>(validated(name))
    class IntKey(name: String) : PreferenceKey<Int>(validated(name))
    class LongKey(name: String) : PreferenceKey<Long>(validated(name))
    class DoubleKey(name: String) : PreferenceKey<Double>(validated(name))

    companion object {
        fun string(name: String): PreferenceKey<String> = StringKey(name)
        fun boolean(name: String): PreferenceKey<Boolean> = BooleanKey(name)
        fun int(name: String): PreferenceKey<Int> = IntKey(name)
        fun long(name: String): PreferenceKey<Long> = LongKey(name)
        fun double(name: String): PreferenceKey<Double> = DoubleKey(name)

        private fun validated(name: String): String {
            require(name.isNotBlank()) { "Preference key must not be blank" }
            require(name.length <= 128) { "Preference key must be <= 128 characters" }
            require(name.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }) {
                "Preference key contains unsupported characters"
            }
            return name
        }
    }
}

/**
 * Canonical non-sensitive application preference storage.
 *
 * Credentials, authentication tokens, payment secrets and other sensitive values must use the
 * secure-storage/session architecture instead of this contract.
 */
interface PreferenceStore {
    fun <T> observe(key: PreferenceKey<T>): Flow<T?>
    suspend fun <T> get(key: PreferenceKey<T>): T?
    suspend fun <T> put(key: PreferenceKey<T>, value: T)
    suspend fun <T> remove(key: PreferenceKey<T>)
    suspend fun clear()
}
