package com.carbroz.partner.sdui.host.cache

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LruMemoryCacheTest {

    @Test
    fun get_returnsNull_whenKeyDoesNotExist() = runTest {
        val cache = LruMemoryCache(maxEntries = 3)
        val key = SduiCacheKey(SduiCacheKey.SessionScope.ANONYMOUS, "/api/v1/sdui/root")
        assertNull(cache.get(key))
    }

    @Test
    fun putAndGet_storesAndRetrievesPayload() = runTest {
        val cache = LruMemoryCache(maxEntries = 3)
        val key = SduiCacheKey(SduiCacheKey.SessionScope.ANONYMOUS, "/api/v1/sdui/root")
        val json = """{"screen": "login"}"""

        cache.put(key, json)
        assertEquals(json, cache.get(key))
    }

    @Test
    fun put_enforcesMaxEntriesCapacityEviction() = runTest {
        val cache = LruMemoryCache(maxEntries = 2)
        val key1 = SduiCacheKey(SduiCacheKey.SessionScope.ANONYMOUS, "/endpoint1")
        val key2 = SduiCacheKey(SduiCacheKey.SessionScope.ANONYMOUS, "/endpoint2")
        val key3 = SduiCacheKey(SduiCacheKey.SessionScope.ANONYMOUS, "/endpoint3")

        cache.put(key1, "json1")
        cache.put(key2, "json2")
        cache.put(key3, "json3")

        assertEquals(2, cache.size())
        assertNull(cache.get(key1))
        assertEquals("json2", cache.get(key2))
        assertEquals("json3", cache.get(key3))
    }

    @Test
    fun clearAuthenticated_removesOnlyAuthenticatedEntries() = runTest {
        val cache = LruMemoryCache(maxEntries = 5)
        val anonKey = SduiCacheKey(SduiCacheKey.SessionScope.ANONYMOUS, "/anon")
        val authKey = SduiCacheKey(SduiCacheKey.SessionScope.AUTHENTICATED, "/auth")

        cache.put(anonKey, "anon_json")
        cache.put(authKey, "auth_json")

        cache.clearAuthenticated()

        assertEquals("anon_json", cache.get(anonKey))
        assertNull(cache.get(authKey))
    }
}
