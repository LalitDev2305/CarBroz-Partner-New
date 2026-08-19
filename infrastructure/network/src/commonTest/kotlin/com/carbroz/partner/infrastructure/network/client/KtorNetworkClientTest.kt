package com.carbroz.partner.infrastructure.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KtorNetworkClientTest {

    @Test
    fun testRelativeUrlNormalizedWithBaseUrl() = runTest {
        var requestedUrl = ""
        val mockEngine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorNetworkClient(
            baseUrl = "https://api.carbroz.com/",
            httpClient = HttpClient(mockEngine)
        )

        val response = client.execute(NetworkRequest(url = "/api/v1/sdui/root"))
        assertEquals("https://api.carbroz.com/api/v1/sdui/root", requestedUrl)
        assertTrue(response.isSuccessful)
        assertEquals(200, response.statusCode)
        assertEquals("""{"status":"ok"}""", response.bodyJson)
    }

    @Test
    fun testAbsoluteUrlPreservedUnchanged() = runTest {
        var requestedUrl = ""
        val mockEngine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK
            )
        }

        val client = KtorNetworkClient(
            baseUrl = "https://api.carbroz.com",
            httpClient = HttpClient(mockEngine)
        )

        val response = client.execute(NetworkRequest(url = "https://custom.external.com/data"))
        assertEquals("https://custom.external.com/data", requestedUrl)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun testHttpErrorStatusMapping() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":"Not Found"}""",
                status = HttpStatusCode.NotFound
            )
        }

        val client = KtorNetworkClient(
            baseUrl = "https://api.carbroz.com",
            httpClient = HttpClient(mockEngine)
        )

        val response = client.execute(NetworkRequest(url = "/missing"))
        assertFalse(response.isSuccessful)
        assertEquals(404, response.statusCode)
        assertEquals("""{"error":"Not Found"}""", response.bodyJson)
    }

    @Test
    fun testNetworkExceptionMapping() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Connection timed out")
        }

        val client = KtorNetworkClient(
            baseUrl = "https://api.carbroz.com",
            httpClient = HttpClient(mockEngine)
        )

        val response = client.execute(NetworkRequest(url = "/timeout"))
        assertFalse(response.isSuccessful)
        assertEquals(500, response.statusCode)
        assertTrue(response.bodyJson.contains("Connection timed out"))
    }
}
