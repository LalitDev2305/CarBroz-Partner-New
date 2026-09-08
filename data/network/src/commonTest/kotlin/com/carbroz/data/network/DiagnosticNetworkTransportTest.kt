package com.carbroz.data.network

import com.carbroz.foundation.observability.DiagnosticBlock
import com.carbroz.foundation.observability.DiagnosticBlockKind
import com.carbroz.foundation.observability.DiagnosticBlockSink
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticNetworkTransportTest {
    @Test
    fun `request and response are emitted once with pretty json and sensitive values redacted`() = runTest {
        val blocks = mutableListOf<DiagnosticBlock>()
        var delegateCalls = 0
        val delegate = NetworkTransport { request ->
            delegateCalls += 1
            assertEquals("POST", request.method)
            NetworkResult.Success(
                NetworkResponse(
                    statusCode = 200,
                    body = buildJsonObject {
                        put("success", true)
                        put("phoneNumber", "9999999999")
                        put("screenId", "partner_login")
                    },
                ),
            )
        }
        val transport = DiagnosticNetworkTransport(
            delegate = delegate,
            sink = DiagnosticBlockSink(blocks::add),
        )

        val result = transport.execute(
            TransportRequest(
                method = "POST",
                url = "https://example.test/api/v1/partner/auth?token=secret&mode=test",
                headers = mapOf(
                    "Authorization" to "Bearer secret-token",
                    "X-CarBroz-Platform" to "ANDROID",
                ),
                body = """{"phoneNumber":"9999999999","name":"Partner","otp":"123456"}""",
            ),
        )

        assertTrue(result is NetworkResult.Success)
        assertEquals(1, delegateCalls)
        assertEquals(2, blocks.size)
        assertEquals(DiagnosticBlockKind.API_REQUEST, blocks[0].kind)
        assertEquals(DiagnosticBlockKind.API_RESPONSE, blocks[1].kind)

        val requestText = blocks[0].title + blocks[0].content
        assertTrue("token=[REDACTED]" in requestText)
        assertTrue("X-CarBroz-Platform" in requestText)
        assertTrue("ANDROID" in requestText)
        assertTrue("Partner" in requestText)
        assertFalse("secret-token" in requestText)
        assertFalse("9999999999" in requestText)
        assertFalse("123456" in requestText)
        assertTrue("[REDACTED]" in requestText)

        val responseText = blocks[1].content
        assertTrue("partner_login" in responseText)
        assertFalse("9999999999" in responseText)
        assertTrue("\n" in responseText)
    }

    @Test
    fun `http failure emits one api error block and preserves response structure`() = runTest {
        val blocks = mutableListOf<DiagnosticBlock>()
        val transport = DiagnosticNetworkTransport(
            delegate = NetworkTransport {
                NetworkResult.Failure(
                    NetworkFailure.Http(
                        statusCode = 400,
                        body = buildJsonObject {
                            put("code", "INVALID_REQUEST")
                            put("email", "person@example.test")
                        },
                    ),
                )
            },
            sink = DiagnosticBlockSink(blocks::add),
        )

        transport.execute(
            TransportRequest(
                method = "GET",
                url = "https://example.test/api/v1/partner/bootstrap",
                headers = emptyMap(),
                body = null,
            ),
        )

        assertEquals(2, blocks.size)
        assertEquals(DiagnosticBlockKind.API_ERROR, blocks.last().kind)
        assertTrue("INVALID_REQUEST" in blocks.last().content)
        assertFalse("person@example.test" in blocks.last().content)
    }
}
