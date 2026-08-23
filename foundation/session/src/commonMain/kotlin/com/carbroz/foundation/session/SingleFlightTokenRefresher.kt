package com.carbroz.foundation.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ensures concurrent refresh requests share one in-flight refresh operation.
 *
 * The underlying [TokenRefresher] is invoked at most once for a concurrently
 * overlapping refresh burst. Callers receive the same result. Cancellation of
 * one waiter does not cancel the shared refresh operation, while cancellation
 * of the refresh scope itself is propagated to all waiters.
 */
class SingleFlightTokenRefresher(
    private val delegate: TokenRefresher,
    private val scope: CoroutineScope,
) : TokenRefresher {
    private val mutex = Mutex()
    private var inFlight: CompletableDeferred<TokenRefreshResult>? = null

    override suspend fun refresh(current: AuthTokens): TokenRefreshResult {
        val deferred = mutex.withLock {
            inFlight?.let { return@withLock it }

            CompletableDeferred<TokenRefreshResult>().also { created ->
                inFlight = created
                scope.launch(SupervisorJob()) {
                    try {
                        created.complete(delegate.refresh(current))
                    } catch (cancellation: CancellationException) {
                        created.completeExceptionally(cancellation)
                        throw cancellation
                    } catch (throwable: Throwable) {
                        created.complete(
                            TokenRefreshResult.Failed(
                                TokenRefreshFailure.Unexpected(
                                    throwable.message ?: throwable::class.simpleName ?: "Unexpected refresh failure",
                                ),
                            ),
                        )
                    } finally {
                        mutex.withLock {
                            if (inFlight === created) {
                                inFlight = null
                            }
                        }
                    }
                }
            }
        }

        return deferred.await()
    }
}
