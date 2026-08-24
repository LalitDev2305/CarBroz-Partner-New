package com.carbroz.data.preferences

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InMemoryPreferenceStoreTest {
    @Test
    fun typedValuesRoundTripAndWrongTypeReadsAsAbsent() = runTest {
        val store = InMemoryPreferenceStore()

        store.putString("ui.theme", "dark")
        store.putBoolean("ui.compact", true)
        store.putInt("list.page_size", 25)
        store.putLong("sync.last_seen", 42L)
        store.putDouble("map.zoom", 12.5)

        assertEquals("dark", store.getString("ui.theme"))
        assertEquals(true, store.getBoolean("ui.compact"))
        assertEquals(25, store.getInt("list.page_size"))
        assertEquals(42L, store.getLong("sync.last_seen"))
        assertEquals(12.5, store.getDouble("map.zoom"))
        assertNull(store.getBoolean("ui.theme"))
    }

    @Test
    fun observationReflectsCurrentValueRemovalAndClear() = runTest {
        val store = InMemoryPreferenceStore(mapOf("ui.theme" to PreferenceValue.StringValue("light")))

        assertEquals("light", store.observeString("ui.theme").first())
        store.remove("ui.theme")
        assertNull(store.observeString("ui.theme").first())

        store.putString("ui.theme", "dark")
        store.putInt("list.page_size", 50)
        store.clear()
        assertNull(store.getString("ui.theme"))
        assertNull(store.getInt("list.page_size"))
    }

    @Test
    fun invalidKeysAreRejectedAtEveryBoundary() = runTest {
        val store = InMemoryPreferenceStore()

        assertFailsWith<IllegalArgumentException> { store.getString(" ") }
        assertFailsWith<IllegalArgumentException> { store.putString("bad/key", "value") }
        assertFailsWith<IllegalArgumentException> { store.observeString("bad key") }
        assertFailsWith<IllegalArgumentException> { store.remove("?") }
    }
}
