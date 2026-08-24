package com.carbroz.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe shared implementation used by tests and by hosts that intentionally choose
 * process-lifetime non-sensitive preferences.
 */
class InMemoryPreferenceStore(
    initialValues: Map<String, PreferenceValue> = emptyMap(),
) : PreferenceStore {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initialValues.toMap())

    override fun observeString(key: String): Flow<String?> = observe(key) { (it as? PreferenceValue.StringValue)?.value }
    override fun observeBoolean(key: String): Flow<Boolean?> = observe(key) { (it as? PreferenceValue.BooleanValue)?.value }
    override fun observeInt(key: String): Flow<Int?> = observe(key) { (it as? PreferenceValue.IntValue)?.value }
    override fun observeLong(key: String): Flow<Long?> = observe(key) { (it as? PreferenceValue.LongValue)?.value }
    override fun observeDouble(key: String): Flow<Double?> = observe(key) { (it as? PreferenceValue.DoubleValue)?.value }

    override suspend fun getString(key: String): String? = get(key) { (it as? PreferenceValue.StringValue)?.value }
    override suspend fun getBoolean(key: String): Boolean? = get(key) { (it as? PreferenceValue.BooleanValue)?.value }
    override suspend fun getInt(key: String): Int? = get(key) { (it as? PreferenceValue.IntValue)?.value }
    override suspend fun getLong(key: String): Long? = get(key) { (it as? PreferenceValue.LongValue)?.value }
    override suspend fun getDouble(key: String): Double? = get(key) { (it as? PreferenceValue.DoubleValue)?.value }

    override suspend fun putString(key: String, value: String) = put(key, PreferenceValue.StringValue(value))
    override suspend fun putBoolean(key: String, value: Boolean) = put(key, PreferenceValue.BooleanValue(value))
    override suspend fun putInt(key: String, value: Int) = put(key, PreferenceValue.IntValue(value))
    override suspend fun putLong(key: String, value: Long) = put(key, PreferenceValue.LongValue(value))
    override suspend fun putDouble(key: String, value: Double) = put(key, PreferenceValue.DoubleValue(value))

    override suspend fun remove(key: String) {
        requireValidPreferenceKey(key)
        mutex.withLock { state.value = state.value - key }
    }

    override suspend fun clear() {
        mutex.withLock { state.value = emptyMap() }
    }

    private fun <T> observe(key: String, transform: (PreferenceValue?) -> T): Flow<T> {
        requireValidPreferenceKey(key)
        return state.map { values -> transform(values[key]) }
    }

    private suspend fun <T> get(key: String, transform: (PreferenceValue?) -> T): T {
        requireValidPreferenceKey(key)
        return mutex.withLock { transform(state.value[key]) }
    }

    private suspend fun put(key: String, value: PreferenceValue) {
        requireValidPreferenceKey(key)
        mutex.withLock { state.value = state.value + (key to value) }
    }
}

sealed interface PreferenceValue {
    data class StringValue(val value: String) : PreferenceValue
    data class BooleanValue(val value: Boolean) : PreferenceValue
    data class IntValue(val value: Int) : PreferenceValue
    data class LongValue(val value: Long) : PreferenceValue
    data class DoubleValue(val value: Double) : PreferenceValue
}
