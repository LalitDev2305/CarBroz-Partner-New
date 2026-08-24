package com.carbroz.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Composition-facing source of the canonical non-sensitive preference store. */
fun interface PreferenceStoreProvider {
    fun get(): PreferenceStore
}

/** Creates the shared DataStore runtime over a platform-provided Preferences storage backend. */
fun createPreferencesDataStore(storage: Storage<Preferences>): DataStore<Preferences> =
    DataStoreFactory.create(storage = storage)

/** DataStore-backed implementation of [PreferenceStore]. */
class DataStorePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : PreferenceStore {
    override fun <T> observe(key: PreferenceKey<T>): Flow<T?> =
        dataStore.data.map { preferences -> preferences[nativeKey(key)] }

    override suspend fun <T> get(key: PreferenceKey<T>): T? =
        dataStore.data.first()[nativeKey(key)]

    override suspend fun <T> put(key: PreferenceKey<T>, value: T) {
        dataStore.edit { preferences -> preferences[nativeKey(key)] = value }
    }

    override suspend fun <T> remove(key: PreferenceKey<T>) {
        dataStore.edit { preferences -> preferences.remove(nativeKey(key)) }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.asMap().keys.forEach(preferences::remove) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> nativeKey(key: PreferenceKey<T>): Preferences.Key<T> = when (key) {
        is PreferenceKey.StringKey -> stringPreferencesKey(key.name)
        is PreferenceKey.BooleanKey -> booleanPreferencesKey(key.name)
        is PreferenceKey.IntKey -> intPreferencesKey(key.name)
        is PreferenceKey.LongKey -> longPreferencesKey(key.name)
        is PreferenceKey.DoubleKey -> doublePreferencesKey(key.name)
    } as Preferences.Key<T>
}

internal const val PREFERENCES_FILE_NAME = "carbroz.preferences_pb"
