package com.carbroz.data.network

import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkCacheTest {
    @Test
    fun cacheFirstReturnsFreshCachedResponseWithoutTransportCall() = runTest {
        val clock = MutableClock(1_000L)
        val cache = InMemoryNetworkResponseCache()
        val transport = RecordingTransport(NetworkResult.Success(response("network")))
        val executor = executor(transport, cache, clock)
        val request = request(NetworkCachePolicy.CacheFirst(maxAgeMillis = 5_000L))

        cache.put(
            NetworkCacheKey(request.endpoint),
            NetworkCacheEntry(response("cached"), storedAtEpochMillis = 500L),
        )

        val result = executor.execute(request)

        assertEquals("cached", result.bodyValue())
        assertEquals(0, transport.calls)
    }

    @Test
    fun expiredCacheRefreshesFromNetworkAndStoresNewResponse() = runTest {
        val clock = MutableClock(10_000L)
        val cache = InMemoryNetworkResponseCache()
        val transport = RecordingTransport(NetworkResult.Success(response("network")))
        val executor = executor(transport, cache, clock)
        val request = request(NetworkCachePolicy.CacheFirst(maxAgeMillis = 1_000L))
        val key = NetworkCacheKey(request.endpoint)
        cache.put(key, NetworkCacheEntry(response("old"), storedAtEpochMillis = 1_000L))

        val result = executor.execute(request)

        assertEquals("network", result.bodyValue())
        assertEquals(1, transport.calls)
        assertEquals("network", NetworkResult.Success(cache.get(key)!!.response).bodyValue())
    }

    @Test
    fun networkFirstFallsBackToAcceptablyAgedCacheOnFailure() = runTest {
        val clock = MutableClock(10_000L)
        val cache = InMemoryNetworkResponseCache()
        val transport = RecordingTransport(NetworkResult.Failure(NetworkFailure.Offline))
        val executor = executor(transport, cache, clock)
        val request = request(NetworkCachePolicy.NetworkFirst(fallbackMaxAgeMillis = 10_000L))
        cache.put(
            NetworkCacheKey(request.endpoint),
            NetworkCacheEntry(response("fallback"), storedAtEpochMillis = 1_000L),
        )

        val result = executor.execute(request)

        assertEquals("fallback", result.bodyValue())
        assertEquals(1, transport.calls)
    }

    @Test
    fun noStoreResponseIsNeverWrittenToCache() = runTest {
        val clock = MutableClock(1_000L)
        val cache = InMemoryNetworkResponseCache()
        val transport = RecordingTransport(
            NetworkResult.Success(
                response("private", headers = mapOf("Cache-Control" to "private, no-store")),
            ),
        )
        val executor = executor(transport, cache, clock)
        val request = request(NetworkCachePolicy.CacheFirst(maxAgeMillis = 5_000L))

        executor.execute(request)

        assertEquals(null, cache.get(NetworkCacheKey(request.endpoint)))
    }

    @Test
    fun authenticatedOrMutatingRequestsCannotUseSharedResponseCache() = runTest {
        val executor = executor(RecordingTransport(NetworkResult.Success(response("unused"))), InMemoryNetworkResponseCache(), MutableClock(0L))

        val authenticated = executor.execute(
            request(NetworkCachePolicy.CacheFirst(1_000L)).copy(authentication = NetworkAuthentication.SESSION),
        )
        val mutation = executor.execute(
            NetworkRequest(
                method = NetworkMethod.POST,
                endpoint = NetworkEndpoint("/screen"),
                cachePolicy = NetworkCachePolicy.NetworkFirst(1_000L),
            ),
        )

        assertIs<NetworkFailure.InvalidRequest>((authenticated as NetworkResult.Failure).error)
        assertIs<NetworkFailure.InvalidRequest>((mutation as NetworkResult.Failure).error)
    }

    @Test
    fun boundedInMemoryCacheEvictsLeastRecentlyUsedEntry() = runTest {
        val cache = InMemoryNetworkResponseCache(maxEntries = 2)
        val first = NetworkCacheKey(NetworkEndpoint("/one"))
        val second = NetworkCacheKey(NetworkEndpoint("/two"))
        val third = NetworkCacheKey(NetworkEndpoint("/three"))

        cache.put(first, NetworkCacheEntry(response("1"), 0L))
        cache.put(second, NetworkCacheEntry(response("2"), 0L))
        cache.get(first)
        cache.put(third, NetworkCacheEntry(response("3"), 0L))

        assertEquals(null, cache.get(second))
        assertEquals("1", NetworkResult.Success(cache.get(first)!!.response).bodyValue())
        assertEquals("3", NetworkResult.Success(cache.get(third)!!.response).bodyValue())
    }

    private fun executor(
        transport: NetworkTransport,
        cache: NetworkResponseCache,
        clock: Clock,
    ) = NetworkExecutor(
        environment = NetworkEnvironment("https://api.example.com"),
        transport = transport,
        responseCache = cache,
        clock = clock,
    )

    private fun request(policy: NetworkCachePolicy) = NetworkRequest(
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/screen"),
        cachePolicy = policy,
    )

    private fun response(value: String, headers: Map<String, String> = emptyMap()) = NetworkResponse(
        statusCode = 200,
        headers = headers,
        body = JsonPrimitive(value),
    )

    private fun NetworkResult.bodyValue(): String =
        ((this as NetworkResult.Success).response.body as JsonPrimitive).content

    private class MutableClock(private var now: Long) : Clock {
        override fun nowEpochMilliseconds(): Long = now
    }

    private class RecordingTransport(private val result: NetworkResult) : NetworkTransport {
        var calls: Int = 0
            private set

        override suspend fun execute(request: TransportRequest): NetworkResult {
            calls += 1
            return result
        }
    }
}
