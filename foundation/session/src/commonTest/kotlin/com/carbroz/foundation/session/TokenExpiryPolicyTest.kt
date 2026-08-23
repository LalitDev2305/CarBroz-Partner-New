package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TokenExpiryPolicyTest {

    @Test
    fun `returns unknown when access token expiry is absent`() {
        val policy = TokenExpiryPolicy(clock = FixedClock(1_000L))

        assertEquals(TokenExpiryState.Unknown, policy.evaluate(tokens(expiresAt = null)))
    }

    @Test
    fun `returns expired at exact expiry instant`() {
        val policy = TokenExpiryPolicy(clock = FixedClock(10_000L))

        assertEquals(TokenExpiryState.Expired, policy.evaluate(tokens(expiresAt = 10_000L)))
    }

    @Test
    fun `returns expired after expiry instant`() {
        val policy = TokenExpiryPolicy(clock = FixedClock(10_001L))

        assertEquals(TokenExpiryState.Expired, policy.evaluate(tokens(expiresAt = 10_000L)))
    }

    @Test
    fun `recommends refresh at exact skew boundary`() {
        val policy = TokenExpiryPolicy(
            clock = FixedClock(10_000L),
            refreshSkewMilliseconds = 5_000L,
        )

        assertEquals(TokenExpiryState.RefreshRecommended, policy.evaluate(tokens(expiresAt = 15_000L)))
    }

    @Test
    fun `returns valid outside refresh skew`() {
        val policy = TokenExpiryPolicy(
            clock = FixedClock(10_000L),
            refreshSkewMilliseconds = 5_000L,
        )

        assertEquals(TokenExpiryState.Valid, policy.evaluate(tokens(expiresAt = 15_001L)))
    }

    @Test
    fun `zero skew only refreshes once token is expired`() {
        val policy = TokenExpiryPolicy(
            clock = FixedClock(10_000L),
            refreshSkewMilliseconds = 0L,
        )

        assertEquals(TokenExpiryState.Valid, policy.evaluate(tokens(expiresAt = 10_001L)))
        assertEquals(TokenExpiryState.Expired, policy.evaluate(tokens(expiresAt = 10_000L)))
    }

    @Test
    fun `negative refresh skew is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            TokenExpiryPolicy(
                clock = FixedClock(0L),
                refreshSkewMilliseconds = -1L,
            )
        }
    }

    private fun tokens(expiresAt: Long?): AuthTokens = AuthTokens(
        accessToken = Secret.of("access-token"),
        refreshToken = Secret.of("refresh-token"),
        accessTokenExpiresAtEpochMilliseconds = expiresAt,
    )

    private class FixedClock(private val now: Long) : Clock {
        override fun nowEpochMilliseconds(): Long = now
    }
}
