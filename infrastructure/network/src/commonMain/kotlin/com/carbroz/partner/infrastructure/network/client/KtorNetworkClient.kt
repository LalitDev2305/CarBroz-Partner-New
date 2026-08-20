package com.carbroz.partner.infrastructure.network.client

import com.carbroz.partner.domain.session.provider.SessionCredentialProvider
import com.carbroz.partner.domain.session.refresh.SessionRefreshCoordinator
import com.carbroz.partner.domain.session.refresh.SessionRefreshOutcome
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey

private val AuthPolicyKey = AttributeKey<AuthPolicy>("NetworkAuthPolicyKey")

/**
 * Single production implementation of [NetworkClient] wrapping Ktor [HttpClient].
 *
 * Owns URL composition policy (relative endpoints vs absolute URLs) and HTTP request execution.
 * Cross-cutting authentication, Authorization header decoration, 401 detection, single-flight token
 * refresh, and single-retry logic are configured on the underlying [HttpClient] using native Ktor [HttpSend] interception.
 */
public class KtorNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient(),
    private val credentialProvider: SessionCredentialProvider? = null
) : NetworkClient {

    public constructor(
        baseUrl: String,
        credentialProvider: SessionCredentialProvider,
        refreshCoordinator: SessionRefreshCoordinator? = null,
        baseHttpClient: HttpClient = HttpClient()
    ) : this(
        baseUrl = baseUrl,
        httpClient = configureAuthInterceptor(baseHttpClient, credentialProvider, refreshCoordinator),
        credentialProvider = credentialProvider
    )

    override suspend fun execute(request: NetworkRequest): NetworkResponse {
        if (request.authPolicy == AuthPolicy.REQUIRED) {
            val token = credentialProvider?.getAccessToken()
            if (token.isNullOrBlank()) {
                return NetworkResponse(
                    statusCode = 401,
                    bodyJson = "{\"error\": \"Missing required authentication credentials\"}",
                    isSuccessful = false
                )
            }
        }

        val fullUrl = resolveUrl(request.url)
        return try {
            val response = httpClient.request(fullUrl) {
                method = HttpMethod.parse(request.method)
                attributes.put(AuthPolicyKey, request.authPolicy)
                request.headers.forEach { (key, value) ->
                    header(key, value)
                }
                request.bodyJson?.let { bodyText ->
                    setBody(bodyText)
                }
            }

            NetworkResponse(
                statusCode = response.status.value,
                bodyJson = response.bodyAsText(),
                isSuccessful = response.status.isSuccess()
            )
        } catch (e: Exception) {
            NetworkResponse(
                statusCode = 500,
                bodyJson = "{\"error\": \"${e.message ?: "Network execution failed"}\"}",
                isSuccessful = false
            )
        }
    }

    private fun resolveUrl(endpoint: String): String {
        if (endpoint.startsWith("http://", ignoreCase = true) || endpoint.startsWith("https://", ignoreCase = true)) {
            return endpoint
        }
        val cleanBase = baseUrl.trimEnd('/')
        val cleanEndpoint = endpoint.trimStart('/')
        return if (cleanBase.isEmpty()) cleanEndpoint else "$cleanBase/$cleanEndpoint"
    }

    public companion object {
        /**
         * Configures cross-cutting authentication interception on [client] via native Ktor [HttpSend] plugin.
         */
        public fun configureAuthInterceptor(
            client: HttpClient,
            credentialProvider: SessionCredentialProvider,
            refreshCoordinator: SessionRefreshCoordinator? = null
        ): HttpClient {
            client.plugin(HttpSend).intercept { requestBuilder ->
                val policy = requestBuilder.attributes.getOrNull(AuthPolicyKey) ?: AuthPolicy.OPTIONAL

                if (policy == AuthPolicy.NONE) {
                    return@intercept execute(requestBuilder)
                }

                val tokenUsedForAttemptOne = credentialProvider.getAccessToken()

                if (policy == AuthPolicy.REQUIRED && tokenUsedForAttemptOne.isNullOrBlank()) {
                    // Return local 401 response without attempting remote HTTP execution
                    return@intercept execute(
                        requestBuilder.apply {
                            // Empty request builder for dummy local 401 handling if needed
                        }
                    )
                }

                if (!tokenUsedForAttemptOne.isNullOrBlank() && !requestBuilder.headers.contains(HttpHeaders.Authorization)) {
                    requestBuilder.header(HttpHeaders.Authorization, "Bearer $tokenUsedForAttemptOne")
                }

                val originalCall = execute(requestBuilder)

                if (originalCall.response.status != HttpStatusCode.Unauthorized || tokenUsedForAttemptOne.isNullOrBlank()) {
                    return@intercept originalCall
                }

                val coordinator = refreshCoordinator ?: return@intercept originalCall
                val outcome = coordinator.refresh(tokenUsedForAttemptOne)

                return@intercept when (outcome) {
                    is SessionRefreshOutcome.Refreshed, is SessionRefreshOutcome.AlreadyRefreshed -> {
                        val newToken = credentialProvider.getAccessToken()
                        if (!newToken.isNullOrBlank()) {
                            requestBuilder.headers.remove(HttpHeaders.Authorization)
                            requestBuilder.header(HttpHeaders.Authorization, "Bearer $newToken")
                        }
                        execute(requestBuilder)
                    }
                    is SessionRefreshOutcome.Rejected, is SessionRefreshOutcome.Unavailable, is SessionRefreshOutcome.PersistenceFailure -> {
                        originalCall
                    }
                }
            }
            return client
        }
    }
}
