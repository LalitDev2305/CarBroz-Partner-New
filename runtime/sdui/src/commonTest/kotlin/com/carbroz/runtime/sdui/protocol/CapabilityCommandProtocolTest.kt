package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CapabilityCommandProtocolTest {
    private val json = Json { classDiscriminator = "kind" }

    @Test
    fun capabilityCommandDecodesFromSemanticWireContract() {
        val command = json.decodeFromString<CommandDto>(
            """{"kind":"CAPABILITY","capability":"sharing","operation":"share","arguments":{"text":"hello"}}""",
        )

        val capability = assertIs<CapabilityCommandDto>(command)
        assertEquals("sharing", capability.capability)
        assertEquals("share", capability.operation)
        assertEquals("hello", capability.arguments["text"]?.toString()?.trim('"'))
    }
}
