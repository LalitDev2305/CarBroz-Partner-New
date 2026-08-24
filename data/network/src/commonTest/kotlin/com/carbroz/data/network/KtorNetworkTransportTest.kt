package com.carbroz.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class KtorNetworkTransportTest {
    @Test
    fun successMapsRequestAndJsonResponse() = runTest {
        val engine = MockEngine { request ->
            assertEquals("POST", request.method.value)
            assertEquals("https://api.carbroz.example/action", request.url.toString())
            assertEquals("request-123", request.headers["X-Request-Id"])
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val transport = KtorNetworkTransport(HttpClient(engine))

        val result = transport.execute(
            TransportRequest(
                method = "POST",
                url = "https://api.carbroz.example/action",
                headers = mapOf("X-Request-Id" to "request-123"),
                body = """{"value":1}""",
            ),
        )

        val success = assertIs<NetworkResult.Success>(result)
        assertEquals(200, success.response.statusCode)
        assertEquals(
            JsonObject(mapOf("ok" to JsonPrimitive(true))),
            success.response.body,
        )
        transport.close()
    }

    @Test
    fun nonSuccessStatusMapsToHttpFailure() = runTest {
        val transport = KtorNetworkTransport(
            HttpClient(
                MockEngine {
                    respond(
                        content = """{"error":"invalid"}""",
                        status = HttpStatusCode.BadRequest,
                    )
                },
            ),
        )

        val result = transport.execute(
            TransportRequest(
                method = "GET",
                url = "https://api.carbroz.example/action",
                headers = emptyMap(),
                body = null,
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        val http = assertIs<NetworkFailure.Http>(failure.error)
        assertEquals(400, http.statusCode)
        assertEquals(
            JsonObject(mapOf("error" to JsonPrimitive("invalid"))),
            http.body,
        )
        transport.close()
    }

    @Test
    fun transportExceptionIsNormalizedWithoutLeakingException() = runTest {
        val transport = KtorNetworkTransport(
            HttpClient(MockEngine { error("socket failed") }),
        )

        val result = transport.execute(
            TransportRequest(
                method = "GET",
                url = "https://api.carbroz.example/action",
                headers = emptyMap(),
                body = null,
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkFailure.Transport>(failure.error)
        transport.close()
    }

    @Test
    fun cancellationPropagates() = runTest {
        val transport = KtorNetworkTransport(
            HttpClient(MockEngine { throw CancellationException("cancelled") }),
        )

        assertFailsWith<CancellationException> {
            transport.execute(
                TransportRequest(
                    method = "GET",
                    url = "https://api.carbroz.example/action",
                    headers = emptyMap(),
                    body = null,
                ),
            )
        }
        transport.close()
    }
}
