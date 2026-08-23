package com.carbroz.foundation.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ensures concurrent refresh requests share one in-flight refresh operation.
 *
 * The underlying [TokenRefresher] is invoked at most once for a concurrently
 * overlapping refresh burst. Callers receive the same result. Cancellation of
 * one waiter does not cancel the shared refresh operation.
 */
class SingleFlightTokenRefresher(
    private val delegate: TokenRefresher,
) : TokenRefresher {
    private val mutex = Mutex()
    private var inFlight: CompletableDeferred<TokenRefreshResult>? = null

    override suspend fun refresh(current: AuthTokens): TokenRefreshResult {
        val deferred = mutex.withLock {
            inFlight?.let { return@withLock it }

            CompletableDeferred<TokenRefreshResult>().also {
                inFlight = it
            }
        }

        if (!deferred.isCompleted) {
            val shouldExecute = mutex.withLock { inFlight === deferred && !deferred.isCompleted }
            if (shouldExecute) {
                try {
                    deferred.complete(delegate.refresh(current))
                } catch (cancellation: CancellationException) {
                    deferred.completeExceptionally(cancellation)
                    throw cancellation
                } catch (throwable: Throwable) {
                    deferred.complete(
                        TokenRefreshResult.Failed(
                            TokenRefreshFailure.Unexpected(
                                throwable.message ?: throwable::class.simpleName ?: "Unexpected refresh failure",
                            ),
                        ),
                    )
                } finally {
                    mutex.withLock {
                        if (inFlight === deferred) {
                            inFlight = null
                        }
                    }
                }
            }
        }

        return deferred.await()
    }
}
