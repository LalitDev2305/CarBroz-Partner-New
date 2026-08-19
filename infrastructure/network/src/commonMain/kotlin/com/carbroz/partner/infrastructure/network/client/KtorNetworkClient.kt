package com.carbroz.partner.infrastructure.network.client

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess

/**
 * Production implementation of [NetworkClient] wrapping Ktor [HttpClient].
 *
 * Owns URL composition policy: relative endpoints (e.g. `"/api/v1/sdui/root"`)
 * are normalized against [baseUrl]. Absolute URLs (starting with `http://` or `https://`)
 * are preserved unchanged.
 */
public class KtorNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient()
) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): NetworkResponse {
        val fullUrl = resolveUrl(request.url)
        return try {
            val response = httpClient.request(fullUrl) {
                method = HttpMethod.parse(request.method)
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
}
