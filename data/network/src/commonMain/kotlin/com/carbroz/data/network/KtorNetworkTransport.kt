package com.carbroz.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.headers
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Ktor-backed network transport. Ktor types are intentionally contained inside data:network.
 *
 * The transport owns protocol execution only. Request policy, trusted base URL resolution,
 * authentication headers and other higher-level network concerns are applied before this boundary.
 */
class KtorNetworkTransport(
    private val client: HttpClient,
    private val json: Json = Json,
) : NetworkTransport {
    override suspend fun execute(request: TransportRequest): NetworkResult = try {
        val response = client.request(request.url) {
            method = HttpMethod.parse(request.method)
            headers {
                request.headers.forEach { (name, value) -> append(name, value) }
            }
            request.body?.let { body ->
                if (request.headers.keys.none { it.equals(HttpHeaders.ContentType, ignoreCase = true) }) {
                    contentType(ContentType.Application.Json)
                }
                setBody(body)
            }
        }

        val bodyText = response.bodyAsText()
        val body = bodyText.takeIf { it.isNotBlank() }?.let(::decodeBody)
        val networkResponse = NetworkResponse(
            statusCode = response.status.value,
            headers = response.headers.entries().associate { (name, values) ->
                name to values.joinToString(",")
            },
            body = body,
        )

        if (response.status.value in 200..299) {
            NetworkResult.Success(networkResponse)
        } else {
            NetworkResult.Failure(
                NetworkFailure.Http(
                    statusCode = response.status.value,
                    body = body,
                ),
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: HttpRequestTimeoutException) {
        NetworkResult.Failure(NetworkFailure.Timeout)
    } catch (_: Throwable) {
        NetworkResult.Failure(NetworkFailure.Transport())
    }

    fun close() {
        client.close()
    }

    private fun decodeBody(value: String): JsonElement? =
        runCatching { json.parseToJsonElement(value) }.getOrNull()
}

/** Creates the production multiplatform Ktor transport using the CIO engine. */
fun createKtorNetworkTransport(): KtorNetworkTransport =
    KtorNetworkTransport(HttpClient(CIO))
