package com.carbroz.partner.sdui.host.repository

import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.sdui.host.cache.LruMemoryCache
import com.carbroz.partner.sdui.host.cache.SduiCacheKey
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CachedSduiScreenRepositoryTest {

    private class FakeSduiScreenRepository(
        private val responseJson: String = """{"screen": "home"}"""
    ) : SduiScreenRepository {
        var callCount = 0
            private set

        override suspend fun fetchScreenJson(endpoint: String): Result<String> {
            callCount++
            return Result.success(responseJson)
        }
    }

    @Test
    fun fetchScreenJson_cachesResponse_andAvoidsUpstreamOnSecondCall() = runTest {
        val fakeNetwork = FakeSduiScreenRepository()
        val sessionStore = SessionStore()
        val repository = CachedSduiScreenRepository(
            delegate = fakeNetwork,
            sessionStore = sessionStore
        )

        val firstResult = repository.fetchScreenJson("/api/v1/sdui/root")
        assertEquals(1, fakeNetwork.callCount)
        assertEquals("""{"screen": "home"}""", firstResult.getOrNull())

        val secondResult = repository.fetchScreenJson("/api/v1/sdui/root")
        assertEquals(1, fakeNetwork.callCount)
        assertEquals("""{"screen": "home"}""", secondResult.getOrNull())
    }

    @Test
    fun fetchScreenJson_coalescesSimultaneousConcurrentRequests() = runTest {
        val fakeNetwork = FakeSduiScreenRepository()
        val sessionStore = SessionStore()
        val repository = CachedSduiScreenRepository(
            delegate = fakeNetwork,
            sessionStore = sessionStore
        )

        coroutineScope {
            val jobs = List(10) {
                async {
                    repository.fetchScreenJson("/api/v1/sdui/root")
                }
            }
            jobs.forEach { it.await() }
        }

        assertEquals(1, fakeNetwork.callCount)
    }

    @Test
    fun clearCache_purgesAuthenticatedEntriesOnLogout() = runTest {
        val fakeNetwork = FakeSduiScreenRepository()
        val sessionStore = SessionStore()
        val repository = CachedSduiScreenRepository(
            delegate = fakeNetwork,
            sessionStore = sessionStore
        )

        sessionStore.markAuthenticated()
        repository.fetchScreenJson("/api/v1/sdui/dashboard")
        assertEquals(1, fakeNetwork.callCount)

        repository.clearCache(SduiCacheKey.SessionScope.AUTHENTICATED)

        repository.fetchScreenJson("/api/v1/sdui/dashboard")
        assertEquals(2, fakeNetwork.callCount)
    }
}
