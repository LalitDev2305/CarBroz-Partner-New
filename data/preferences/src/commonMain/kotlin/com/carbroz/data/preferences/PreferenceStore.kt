package com.carbroz.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Canonical non-sensitive application preference storage.
 *
 * Credentials, authentication tokens, payment secrets and other sensitive values must use the
 * secure-storage/session architecture instead of this contract.
 */
interface PreferenceStore {
    fun observeString(key: String): Flow<String?>
    fun observeBoolean(key: String): Flow<Boolean?>
    fun observeInt(key: String): Flow<Int?>
    fun observeLong(key: String): Flow<Long?>
    fun observeDouble(key: String): Flow<Double?>

    suspend fun getString(key: String): String?
    suspend fun getBoolean(key: String): Boolean?
    suspend fun getInt(key: String): Int?
    suspend fun getLong(key: String): Long?
    suspend fun getDouble(key: String): Double?

    suspend fun putString(key: String, value: String)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putInt(key: String, value: Int)
    suspend fun putLong(key: String, value: Long)
    suspend fun putDouble(key: String, value: Double)

    suspend fun remove(key: String)
    suspend fun clear()
}

internal fun requireValidPreferenceKey(key: String) {
    require(key.isNotBlank()) { "Preference key must not be blank" }
    require(key.length <= 128) { "Preference key must be <= 128 characters" }
    require(key.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }) {
        "Preference key contains unsupported characters"
    }
}
