package com.carbroz.partner.sdui.host.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Session-isolated cache key representing an SDUI screen request.
 *
 * @param sessionScope Scope of session ([SessionScope.ANONYMOUS] vs [SessionScope.AUTHENTICATED]).
 * @param endpoint Target SDUI screen endpoint path string.
 */
public data class SduiCacheKey(
    val sessionScope: SessionScope,
    val endpoint: String
) {
    public enum class SessionScope {
        ANONYMOUS,
        AUTHENTICATED
    }
}

/**
 * Bounded process-memory LRU cache for raw backend SDUI screen JSON payloads.
 *
 * @param maxEntries Maximum number of raw screen JSON payloads retained in process memory.
 */
public class LruMemoryCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {
    public companion object {
        public const val DEFAULT_MAX_ENTRIES: Int = 20
    }

    private val mutex = Mutex()
    private val keyOrder = mutableListOf<SduiCacheKey>()
    private val cacheMap = mutableMapOf<SduiCacheKey, String>()

    public suspend fun get(key: SduiCacheKey): String? {
        return mutex.withLock {
            val value = cacheMap[key]
            if (value != null) {
                keyOrder.remove(key)
                keyOrder.add(key)
            }
            value
        }
    }

    public suspend fun put(key: SduiCacheKey, value: String) {
        mutex.withLock {
            if (cacheMap.containsKey(key)) {
                keyOrder.remove(key)
            } else if (keyOrder.size >= maxEntries) {
                val eldest = keyOrder.removeAt(0)
                cacheMap.remove(eldest)
            }
            keyOrder.add(key)
            cacheMap[key] = value
        }
    }

    public suspend fun remove(key: SduiCacheKey) {
        mutex.withLock {
            keyOrder.remove(key)
            cacheMap.remove(key)
        }
    }

    public suspend fun clearAuthenticated() {
        mutex.withLock {
            val keysToRemove = keyOrder.filter { it.sessionScope == SduiCacheKey.SessionScope.AUTHENTICATED }
            for (key in keysToRemove) {
                keyOrder.remove(key)
                cacheMap.remove(key)
            }
        }
    }

    public suspend fun clearAll() {
        mutex.withLock {
            keyOrder.clear()
            cacheMap.clear()
        }
    }

    public suspend fun size(): Int {
        return mutex.withLock {
            cacheMap.size
        }
    }
}
