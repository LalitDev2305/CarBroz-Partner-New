package com.carbroz.partner.core.navigation.stack

import com.carbroz.partner.core.navigation.destination.NavDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NavEntryIdGeneratorTest {

    @Test
    fun verifyInjectedDeterministicGeneratorProducesExpectedIDs() {
        val generator = object : NavEntryIdGenerator {
            private var count = 0
            override fun generateId(): String = "custom_${++count}"
        }
        assertEquals("custom_1", generator.generateId())
        assertEquals("custom_2", generator.generateId())
    }

    @Test
    fun verifyDefaultNavEntryIdGeneratorIsInstanceLocal() {
        val gen1 = DefaultNavEntryIdGenerator()
        val gen2 = DefaultNavEntryIdGenerator()

        val id1 = gen1.generateId()
        val id2 = gen2.generateId()

        assertEquals("entry_1", id1)
        assertEquals("entry_1", id2)
    }

    @Test
    fun verifyDefaultGeneratorProducesUniqueMonotonicIDs() {
        val gen = DefaultNavEntryIdGenerator()
        val ids = List(100) { gen.generateId() }

        assertEquals(100, ids.toSet().size)
        assertEquals("entry_1", ids.first())
        assertEquals("entry_100", ids.last())
    }
}

