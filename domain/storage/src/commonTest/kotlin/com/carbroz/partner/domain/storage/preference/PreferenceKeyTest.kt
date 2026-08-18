package com.carbroz.partner.domain.storage.preference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PreferenceKeyTest {

    @Test
    fun verifyStringKeyValidNameAndDefault() {
        val key = PreferenceKey.StringKey("theme_mode", "dark")
        assertEquals("theme_mode", key.name)
        assertEquals("dark", key.defaultValue)
    }

    @Test
    fun verifyBooleanKeyValidNameAndDefault() {
        val key = PreferenceKey.BooleanKey("onboarding_complete", true)
        assertEquals("onboarding_complete", key.name)
        assertEquals(true, key.defaultValue)
    }

    @Test
    fun verifyIntKeyValidNameAndDefault() {
        val key = PreferenceKey.IntKey("sync_interval_sec", 300)
        assertEquals("sync_interval_sec", key.name)
        assertEquals(300, key.defaultValue)
    }

    @Test
    fun verifyLongKeyValidNameAndDefault() {
        val key = PreferenceKey.LongKey("last_sync_timestamp", 1700000000000L)
        assertEquals("last_sync_timestamp", key.name)
        assertEquals(1700000000000L, key.defaultValue)
    }

    @Test
    fun verifyBlankNameRejectionAcrossAllKeyTypes() {
        assertFailsWith<IllegalArgumentException> { PreferenceKey.StringKey("", "default") }
        assertFailsWith<IllegalArgumentException> { PreferenceKey.StringKey("   ", "default") }

        assertFailsWith<IllegalArgumentException> { PreferenceKey.BooleanKey("", false) }
        assertFailsWith<IllegalArgumentException> { PreferenceKey.BooleanKey("   ", false) }

        assertFailsWith<IllegalArgumentException> { PreferenceKey.IntKey("", 0) }
        assertFailsWith<IllegalArgumentException> { PreferenceKey.IntKey("   ", 0) }

        assertFailsWith<IllegalArgumentException> { PreferenceKey.LongKey("", 0L) }
        assertFailsWith<IllegalArgumentException> { PreferenceKey.LongKey("   ", 0L) }
    }

    @Test
    fun verifyValueEqualityAndHashCode() {
        val key1 = PreferenceKey.StringKey("user_name", "john")
        val key2 = PreferenceKey.StringKey("user_name", "john")
        val key3 = PreferenceKey.StringKey("user_name", "jane")

        assertEquals(key1, key2)
        assertEquals(key1.hashCode(), key2.hashCode())
        assertNotEquals(key1, key3)
    }

    @Test
    fun verifyDifferentKeyTypesSameNameNotEqual() {
        val stringKey = PreferenceKey.StringKey("config_id", "123")
        val intKey = PreferenceKey.IntKey("config_id", 123)

        assertNotEquals<Any>(stringKey, intKey)
    }

    @Test
    fun verifyUnicodeKeyNamePreserved() {
        val key = PreferenceKey.StringKey("partner_कार_broz", "val")
        assertEquals("partner_कार_broz", key.name)
    }
}
