package com.carbroz.partner.sdui.host.repository

import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.sdui.host.cache.LruMemoryCache
import com.carbroz.partner.sdui.host.cache.SduiCacheKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decorator over [SduiScreenRepository] providing session-isolated L1 process-memory caching
 * and single-flight request coalescing for concurrent screen fetches.
 *
 * @param delegate Underlying network screen repository.
 * @param sessionStore Single source of truth for runtime session state.
 * @param cache Bounded thread-safe process-memory cache instance.
 */
public class CachedSduiScreenRepository(
    private val delegate: SduiScreenRepository,
    private val sessionStore: SessionStore,
    private val cache: LruMemoryCache = LruMemoryCache()
) : SduiScreenRepository {

    private val inFlightMutex = Mutex()
    private val inFlightRequests = mutableMapOf<SduiCacheKey, CompletableDeferred<Result<String>>>()

    override suspend fun fetchScreenJson(endpoint: String): Result<String> {
        val currentSession = sessionStore.state.value
        val sessionScope = if (currentSession is SessionState.Authenticated) {
            SduiCacheKey.SessionScope.AUTHENTICATED
        } else {
            SduiCacheKey.SessionScope.ANONYMOUS
        }

        val cacheKey = SduiCacheKey(sessionScope = sessionScope, endpoint = endpoint)

        val cachedJson = cache.get(cacheKey)
        if (cachedJson != null) {
            return Result.success(cachedJson)
        }

        val (deferred, isInitiator) = inFlightMutex.withLock {
            val existing = inFlightRequests[cacheKey]
            if (existing != null) {
                existing to false
            } else {
                val newDeferred = CompletableDeferred<Result<String>>()
                inFlightRequests[cacheKey] = newDeferred
                newDeferred to true
            }
        }

        if (isInitiator) {
            val networkResult = delegate.fetchScreenJson(endpoint)
            networkResult.onSuccess { freshJson ->
                cache.put(cacheKey, freshJson)
            }
            deferred.complete(networkResult)
            inFlightMutex.withLock {
                inFlightRequests.remove(cacheKey)
            }
            return networkResult
        } else {
            return deferred.await()
        }
    }

    /**
     * Purges cached screens for the specified [sessionScope].
     */
    public suspend fun clearCache(sessionScope: SduiCacheKey.SessionScope? = null) {
        if (sessionScope == SduiCacheKey.SessionScope.AUTHENTICATED) {
            cache.clearAuthenticated()
        } else {
            cache.clearAll()
        }
    }
}
