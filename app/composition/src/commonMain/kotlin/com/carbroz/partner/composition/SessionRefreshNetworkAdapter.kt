package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkEnvironment
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkResult
import com.carbroz.data.network.NetworkTransport
import com.carbroz.data.network.TransportRequest
import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.session.AuthTokens
import com.carbroz.foundation.session.TokenRefreshFailure
import com.carbroz.foundation.session.TokenRefreshResult
import com.carbroz.foundation.session.TokenRefresher
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/** Process-lifetime scope used only to protect one shared token refresh from waiter cancellation. */
internal class SessionRefreshScope(
    val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob()),
)

/**
 * CarBroz API adapter for exchanging a refresh token for a new token set.
 *
 * The generic session module deliberately owns no HTTP details. This application-composition
 * adapter owns the product endpoint and response contract and uses the raw transport so refresh
 * never recursively enters NetworkExecutor authentication recovery.
 */
internal class CarBrozTokenRefresher(
    private val environment: NetworkEnvironment,
    private val transport: NetworkTransport,
    private val clock: Clock,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TokenRefresher {
    override suspend fun refresh(current: AuthTokens): TokenRefreshResult {
        val refreshToken = current.refreshToken
            ?: return TokenRefreshResult.Failed(TokenRefreshFailure.MissingRefreshToken)

        val requestBody = json.encodeToString(
            RefreshRequest.serializer(),
            RefreshRequest(refreshToken = refreshToken.reveal()),
        )

        val result = try {
            transport.execute(
                TransportRequest(
                    method = "POST",
                    url = environment.resolve(NetworkEndpoint(REFRESH_ENDPOINT)),
                    headers = emptyMap(),
                    body = requestBody,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return TokenRefreshResult.Failed(TokenRefreshFailure.Unexpected)
        }

        return when (result) {
            is NetworkResult.Success -> decodeSuccess(result, current)
            is NetworkResult.Failure -> TokenRefreshResult.Failed(result.error.toRefreshFailure())
        }
    }

    private fun decodeSuccess(
        result: NetworkResult.Success,
        current: AuthTokens,
    ): TokenRefreshResult {
        val body = result.response.body
            ?: return TokenRefreshResult.Failed(TokenRefreshFailure.Rejected)
        val dto = runCatching { json.decodeFromJsonElement<RefreshResponse>(body) }
            .getOrElse { return TokenRefreshResult.Failed(TokenRefreshFailure.Rejected) }
        if (dto.accessToken.isBlank()) {
            return TokenRefreshResult.Failed(TokenRefreshFailure.Rejected)
        }

        val expiresAt = dto.accessTokenExpiresAtEpochMilliseconds
            ?: dto.expiresInSeconds?.takeIf { it > 0L }?.let { seconds ->
                clock.nowEpochMilliseconds() + seconds * 1_000L
            }
            ?: current.accessTokenExpiresAtEpochMilliseconds

        return TokenRefreshResult.Success(
            AuthTokens(
                accessToken = Secret.of(dto.accessToken),
                refreshToken = dto.refreshToken
                    ?.takeIf(String::isNotBlank)
                    ?.let(Secret::of)
                    ?: current.refreshToken,
                accessTokenExpiresAtEpochMilliseconds = expiresAt,
            ),
        )
    }

    private fun NetworkFailure.toRefreshFailure(): TokenRefreshFailure = when (this) {
        NetworkFailure.Offline,
        NetworkFailure.Timeout,
        NetworkFailure.Transport,
        -> TokenRefreshFailure.NetworkUnavailable

        is NetworkFailure.Http -> when (statusCode) {
            401, 403 -> TokenRefreshFailure.InvalidRefreshToken
            else -> TokenRefreshFailure.Rejected
        }

        is NetworkFailure.InvalidRequest -> TokenRefreshFailure.Unexpected
    }

    @Serializable
    private data class RefreshRequest(
        val refreshToken: String,
    )

    @Serializable
    private data class RefreshResponse(
        val accessToken: String,
        val refreshToken: String? = null,
        val accessTokenExpiresAtEpochMilliseconds: Long? = null,
        val expiresInSeconds: Long? = null,
    )

    private companion object {
        const val REFRESH_ENDPOINT = "/api/v1/auth/refresh"
    }
}
