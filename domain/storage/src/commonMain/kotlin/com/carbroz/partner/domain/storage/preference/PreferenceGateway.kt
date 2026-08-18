package com.carbroz.partner.domain.storage.preference

import com.carbroz.partner.domain.storage.core.StorageResult

/**
 * Domain gateway for reading, writing, and removing type-safe application preferences.
 */
interface PreferenceGateway {
    suspend fun <T> get(key: PreferenceKey<T>): StorageResult<T>
    suspend fun <T> put(key: PreferenceKey<T>, value: T): StorageResult<Unit>
    suspend fun <T> remove(key: PreferenceKey<T>): StorageResult<Unit>
}
