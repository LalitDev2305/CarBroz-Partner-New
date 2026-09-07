package com.carbroz.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkHeaderPolicyTest {
    @Test
    fun providerOwnedHeadersCannotBeOverriddenCaseInsensitively() {
        val policy = NetworkHeaderPolicy()

        assertFailsWith<IllegalArgumentException> {
            policy.merge(
                transportHeaders = mapOf("X-CarBroz-App-Version" to "1.0.0"),
                requestHeaders = mapOf("x-carbroz-app-version" to "9.9.9"),
            )
        }
    }

    @Test
    fun nonOwnedRequestHeadersAreMergedNormally() {
        val merged = NetworkHeaderPolicy().merge(
            transportHeaders = mapOf("X-CarBroz-Platform" to "ANDROID"),
            requestHeaders = mapOf("X-Feature-Context" to "bootstrap"),
        )

        assertEquals("ANDROID", merged["X-CarBroz-Platform"])
        assertEquals("bootstrap", merged["X-Feature-Context"])
    }
}
