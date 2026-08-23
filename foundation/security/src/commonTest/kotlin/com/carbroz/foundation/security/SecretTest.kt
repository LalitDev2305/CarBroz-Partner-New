package com.carbroz.foundation.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SecretTest {
    @Test
    fun `toString never exposes raw secret`() {
        val secret = Secret.of("super-secret-token")

        assertEquals("Secret(**redacted**)", secret.toString())
        assertNotEquals("super-secret-token", secret.toString())
        assertEquals("super-secret-token", secret.reveal())
    }

    @Test
    fun `blank secret is rejected`() {
        assertFailsWith<IllegalArgumentException> { Secret.of("   ") }
    }
}
