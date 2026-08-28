package com.carbroz.feature.splash

import com.carbroz.data.preferences.PreferenceKey
import com.carbroz.data.preferences.PreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BootstrapConfigurationCacheTest {
    @Test
    fun `cache round trips only remote configuration envelope`() = runTest {
        val preferences = InMemoryPreferenceStore()
        val cache = PreferenceBackedBootstrapConfigurationCache(preferences)
        val expected = BootstrapRemoteConfiguration(
            version = "cfg-7",
            data = buildJsonObject { put("serviceAreaVersion", 4) },
        )

        cache.write(expected)

        assertEquals(expected, cache.read())
    }

    @Test
    fun `corrupt cached payload is deleted and treated as a cache miss`() = runTest {
        val preferences = InMemoryPreferenceStore()
        preferences.put(
            PreferenceKey.string("bootstrap.remote.configuration.v1"),
            "{not-json",
        )
        val cache = PreferenceBackedBootstrapConfigurationCache(preferences)

        assertNull(cache.read())
        assertNull(preferences.get(PreferenceKey.string("bootstrap.remote.configuration.v1")))
    }

    private class InMemoryPreferenceStore : PreferenceStore {
        private val values = linkedMapOf<String, Any>()
        private val flows = linkedMapOf<String, MutableStateFlow<Any?>>()

        @Suppress("UNCHECKED_CAST")
        override fun <T> observe(key: PreferenceKey<T>): Flow<T?> =
            flows.getOrPut(key.name) { MutableStateFlow(values[key.name]) } as Flow<T?>

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> get(key: PreferenceKey<T>): T? = values[key.name] as T?

        override suspend fun <T> put(key: PreferenceKey<T>, value: T) {
            values[key.name] = value as Any
            flows.getOrPut(key.name) { MutableStateFlow(null) }.value = value
        }

        override suspend fun <T> remove(key: PreferenceKey<T>) {
            values.remove(key.name)
            flows.getOrPut(key.name) { MutableStateFlow(null) }.value = null
        }

        override suspend fun clear() {
            values.clear()
            flows.values.forEach { it.value = null }
        }
    }
}
