package com.carbroz.data.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkSerializationTest {
    @Test
    fun `typed execution decodes successful body with caller serializer`() = runTest {
        val source = NetworkDataSource {
            NetworkResult.Success(
                NetworkResponse(
                    statusCode = 200,
                    body = JsonPrimitive("ok"),
                ),
            )
        }

        val result = source.executeTyped(request(), String.serializer())

        assertEquals("ok", assertIs<NetworkDecodedResult.Success<String>>(result).value)
    }

    @Test
    fun `typed execution fails closed when body is missing`() = runTest {
        val source = NetworkDataSource {
            NetworkResult.Success(NetworkResponse(statusCode = 204, body = null))
        }

        val result = assertIs<NetworkDecodedResult.Failure>(
            source.executeTyped(request(), String.serializer()),
        )

        assertEquals(NetworkDecodeFailure.MissingBody, result.reason)
    }

    @Test
    fun `typed execution fails closed when body cannot decode`() = runTest {
        val source = NetworkDataSource {
            NetworkResult.Success(
                NetworkResponse(
                    statusCode = 200,
                    body = buildJsonObject { put("not", "a string") },
                ),
            )
        }

        val result = assertIs<NetworkDecodedResult.Failure>(
            source.executeTyped(request(), String.serializer()),
        )

        assertEquals(NetworkDecodeFailure.InvalidBody, result.reason)
    }

    @Test
    fun `typed execution preserves canonical network failure`() = runTest {
        val source = NetworkDataSource { NetworkResult.Failure(NetworkFailure.Timeout) }

        val result = assertIs<NetworkDecodedResult.Failure>(
            source.executeTyped(request(), String.serializer()),
        )

        assertEquals(NetworkDecodeFailure.Network(NetworkFailure.Timeout), result.reason)
    }

    @Test
    fun `raw execution remains available for protocol consumers`() = runTest {
        val raw = NetworkResult.Success(
            NetworkResponse(
                statusCode = 200,
                body = buildJsonObject { put("protocol", 1) },
            ),
        )
        val source = NetworkDataSource { raw }

        assertEquals(raw, source.execute(request()))
    }

    private fun request() = NetworkRequest(
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/typed-test"),
    )
}
