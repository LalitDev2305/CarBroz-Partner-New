package com.carbroz.feature.splash

import com.carbroz.data.preferences.PreferenceKey
import com.carbroz.data.preferences.PreferenceStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

interface BootstrapConfigurationCache {
    suspend fun read(): BootstrapRemoteConfiguration?
    suspend fun write(configuration: BootstrapRemoteConfiguration)
    suspend fun clear()
}

class PreferenceBackedBootstrapConfigurationCache(
    private val preferences: PreferenceStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : BootstrapConfigurationCache {
    override suspend fun read(): BootstrapRemoteConfiguration? {
        val encoded = preferences.get(KEY) ?: return null
        val cached = runCatching { json.decodeFromString<CachedConfiguration>(encoded) }.getOrNull()
        if (cached == null || cached.version.isBlank()) {
            preferences.remove(KEY)
            return null
        }
        return BootstrapRemoteConfiguration(cached.version, cached.data)
    }

    override suspend fun write(configuration: BootstrapRemoteConfiguration) {
        preferences.put(
            KEY,
            json.encodeToString(CachedConfiguration(configuration.version, configuration.data)),
        )
    }

    override suspend fun clear() {
        preferences.remove(KEY)
    }

    @Serializable
    private data class CachedConfiguration(
        val version: String,
        val data: JsonObject,
    )

    private companion object {
        val KEY: PreferenceKey<String> = PreferenceKey.string("bootstrap.remote.configuration.v1")
    }
}
