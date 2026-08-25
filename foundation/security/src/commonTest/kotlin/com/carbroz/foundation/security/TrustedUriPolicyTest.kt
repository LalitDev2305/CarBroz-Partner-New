package com.carbroz.foundation.security

import kotlin.test.Test
import kotlin.test.assertIs

class TrustedUriPolicyTest {
    @Test
    fun trustedHttpsAndExplicitSchemesAreAccepted() {
        val policy = TrustedUriPolicy()

        assertIs<TrustedUriDecision.Trusted>(policy.evaluate("https://example.com/path"))
        assertIs<TrustedUriDecision.Trusted>(policy.evaluate("mailto:test@example.com"))
        assertIs<TrustedUriDecision.Trusted>(policy.evaluate("tel:+15551234567"))
    }

    @Test
    fun executableAndMalformedUrisAreRejected() {
        val policy = TrustedUriPolicy()

        assertIs<TrustedUriDecision.Rejected>(policy.evaluate("javascript:alert(1)"))
        assertIs<TrustedUriDecision.Rejected>(policy.evaluate("file:///tmp/secret"))
        assertIs<TrustedUriDecision.Rejected>(policy.evaluate("https://bad host.example/path"))
        assertIs<TrustedUriDecision.Rejected>(policy.evaluate("https://user@example.com/path"))
    }

    @Test
    fun optionalHostAllowListIsEnforced() {
        val policy = TrustedUriPolicy(allowedHosts = setOf("trusted.example"))

        assertIs<TrustedUriDecision.Trusted>(policy.evaluate("https://trusted.example/path"))
        assertIs<TrustedUriDecision.Rejected>(policy.evaluate("https://other.example/path"))
    }
}
