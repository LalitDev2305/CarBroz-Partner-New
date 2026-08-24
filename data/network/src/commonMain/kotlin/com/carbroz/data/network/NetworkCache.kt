package com.carbroz.data.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Explicit request-level cache behavior. Caching is limited to unauthenticated GET requests. */
sealed interface NetworkCachePolicy {
    data object NetworkOnly : NetworkCachePolicy

    data class CacheFirst(val maxAgeMillis: Long) : NetworkCachePolicy {
        init {
            require(maxAgeMillis > 0L) { "Cache max age must be positive" }
        }
    }

    data class NetworkFirst(val fallbackMaxAgeMillis: Long) : NetworkCachePolicy {
        init {
            require(fallbackMaxAgeMillis > 0L) { "Cache fallback max age must be positive" }
        }
    }
}

/** Cache identity derived only from transport-safe request attributes. */
data class NetworkCacheKey(
    val endpoint: NetworkEndpoint,
    val headers: Map<String, String> = emptyMap(),
)

data class NetworkCacheEntry(
    val response: NetworkResponse,
    val storedAtEpochMillis: Long,
)

/** Reusable network-response cache boundary. Persistent implementations may be supplied by composition later. */
interface NetworkResponseCache {
    suspend fun get(key: NetworkCacheKey): NetworkCacheEntry?
    suspend fun put(key: NetworkCacheKey, entry: NetworkCacheEntry)
    suspend fun remove(key: NetworkCacheKey)
    suspend fun clear()
}

object NoNetworkResponseCache : NetworkResponseCache {
    override suspend fun get(key: NetworkCacheKey): NetworkCacheEntry? = null
    override suspend fun put(key: NetworkCacheKey, entry: NetworkCacheEntry) = Unit
    override suspend fun remove(key: NetworkCacheKey) = Unit
    override suspend fun clear() = Unit
}

/** Process-lifetime production cache with deterministic eviction by entry count. */
class InMemoryNetworkResponseCache(
    private val maxEntries: Int = 128,
) : NetworkResponseCache {
    init {
        require(maxEntries > 0) { "Network cache maxEntries must be positive" }
    }

    private val mutex = Mutex()
    private val entries = linkedMapOf<NetworkCacheKey, NetworkCacheEntry>()

    override suspend fun get(key: NetworkCacheKey): NetworkCacheEntry? = mutex.withLock {
        entries.remove(key)?.also { entries[key] = it }
    }

    override suspend fun put(key: NetworkCacheKey, entry: NetworkCacheEntry) {
        mutex.withLock {
            entries.remove(key)
            entries[key] = entry
            while (entries.size > maxEntries) {
                val oldest = entries.keys.firstOrNull() ?: break
                entries.remove(oldest)
            }
        }
    }

    override suspend fun remove(key: NetworkCacheKey) {
        mutex.withLock { entries.remove(key) }
    }

    override suspend fun clear() {
        mutex.withLock { entries.clear() }
    }
}
