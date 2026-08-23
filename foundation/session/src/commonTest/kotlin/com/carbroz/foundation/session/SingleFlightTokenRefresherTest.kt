package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightTokenRefresherTest {

    @Test
    fun `concurrent callers share one delegate refresh and same result`() = runTest {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val expected = TokenRefreshResult.Success(tokens("new-access"))
        val delegate = TokenRefresher {
            calls += 1
            started.complete(Unit)
            gate.await()
            expected
        }
        val refresher = SingleFlightTokenRefresher(delegate, backgroundScope)

        val first = async { refresher.refresh(tokens("old-access")) }
        val second = async { refresher.refresh(tokens("old-access")) }
        runCurrent()
        started.await()

        assertEquals(1, calls)
        gate.complete(Unit)
        runCurrent()

        assertSame(expected, first.await())
        assertSame(expected, second.await())
        assertEquals(1, calls)
    }

    @Test
    fun `cancelled waiter does not cancel shared delegate refresh`() = runTest {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val expected = TokenRefreshResult.Success(tokens("new-access"))
        val delegate = TokenRefresher {
            calls += 1
            started.complete(Unit)
            gate.await()
            expected
        }
        val refresher = SingleFlightTokenRefresher(delegate, backgroundScope)

        val cancelledWaiter = launch { refresher.refresh(tokens("old-access")) }
        val survivingWaiter = async { refresher.refresh(tokens("old-access")) }
        runCurrent()
        started.await()

        assertEquals(1, calls)
        cancelledWaiter.cancelAndJoin()
        gate.complete(Unit)
        runCurrent()

        assertSame(expected, survivingWaiter.await())
        assertEquals(1, calls)
    }

    @Test
    fun `delegate cancellation propagates to all waiters`() = runTest {
        val delegate = TokenRefresher { throw CancellationException("delegate-cancelled") }
        val refresher = SingleFlightTokenRefresher(delegate, backgroundScope)

        assertFailsWith<CancellationException> {
            refresher.refresh(tokens("old-access"))
        }
    }

    @Test
    fun `unexpected delegate throwable becomes typed failure`() = runTest {
        val delegate = TokenRefresher { error("boom") }
        val refresher = SingleFlightTokenRefresher(delegate, backgroundScope)

        val result = refresher.refresh(tokens("old-access"))

        val failed = assertIs<TokenRefreshResult.Failed>(result)
        val unexpected = assertIs<TokenRefreshFailure.Unexpected>(failed.reason)
        assertEquals("boom", unexpected.reason)
    }

    @Test
    fun `completed flight is cleared so later refresh invokes delegate again`() = runTest {
        var calls = 0
        val delegate = TokenRefresher {
            calls += 1
            TokenRefreshResult.Success(tokens("access-$calls"))
        }
        val refresher = SingleFlightTokenRefresher(delegate, backgroundScope)

        val first = refresher.refresh(tokens("old-access"))
        val second = refresher.refresh(tokens("older-access"))

        assertIs<TokenRefreshResult.Success>(first)
        assertIs<TokenRefreshResult.Success>(second)
        assertEquals(2, calls)
    }

    private fun tokens(access: String): AuthTokens = AuthTokens(
        accessToken = Secret.of(access),
        refreshToken = Secret.of("refresh-token"),
        accessTokenExpiresAtEpochMilliseconds = 123_456L,
    )
}
