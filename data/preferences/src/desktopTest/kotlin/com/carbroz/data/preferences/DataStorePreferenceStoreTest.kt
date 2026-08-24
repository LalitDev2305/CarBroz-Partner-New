package com.carbroz.data.preferences

import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStorePreferenceStoreTest {
    @Test
    fun persistsObservesRemovesAndClearsTypedPreferences() = runTest {
        val directory = Files.createTempDirectory("carbroz-preferences-test").toFile()
        val store = DesktopPreferenceStoreProvider(directory).get()

        val theme = PreferenceKey.string("ui.theme")
        val rtl = PreferenceKey.boolean("ui.rtl")
        val launchCount = PreferenceKey.int("app.launch_count")
        val lastSeen = PreferenceKey.long("app.last_seen")
        val scale = PreferenceKey.double("ui.scale")

        assertNull(store.get(theme))
        store.put(theme, "dark")
        store.put(rtl, true)
        store.put(launchCount, 3)
        store.put(lastSeen, 42L)
        store.put(scale, 1.25)

        assertEquals("dark", store.get(theme))
        assertEquals(true, store.observe(rtl).first())
        assertEquals(3, store.get(launchCount))
        assertEquals(42L, store.get(lastSeen))
        assertEquals(1.25, store.get(scale))

        store.remove(theme)
        assertNull(store.get(theme))

        store.clear()
        assertNull(store.get(rtl))
        assertNull(store.get(launchCount))
        assertNull(store.get(lastSeen))
        assertNull(store.get(scale))
    }

    @Test
    fun rejectsInvalidPreferenceKeys() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { PreferenceKey.string(" ") }
        kotlin.test.assertFailsWith<IllegalArgumentException> { PreferenceKey.string("bad/key") }
    }
}
