package com.carbroz.partner.sdui.host.repository

import com.carbroz.partner.infrastructure.network.client.NetworkClient
import com.carbroz.partner.infrastructure.network.client.NetworkRequest
import com.carbroz.partner.infrastructure.network.client.NetworkResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkSduiScreenRepositoryTest {

    private class FakeNetworkClient(
        private val responseBuilder: (NetworkRequest) -> NetworkResponse
    ) : NetworkClient {
        var lastRequest: NetworkRequest? = null

        override suspend fun execute(request: NetworkRequest): NetworkResponse {
            lastRequest = request
            return responseBuilder(request)
        }
    }

    @Test
    fun testFetchScreenJsonDelegatesEndpointToNetworkClient() = runTest {
        val fakeNetwork = FakeNetworkClient { req ->
            NetworkResponse(
                statusCode = 200,
                bodyJson = """{"screen_id":"home"}""",
                isSuccessful = true
            )
        }
        val repository = NetworkSduiScreenRepository(fakeNetwork)

        val result = repository.fetchScreenJson("/api/v1/sdui/home")

        assertTrue(result.isSuccess)
        assertEquals("""{"screen_id":"home"}""", result.getOrNull())
        assertEquals("/api/v1/sdui/home", fakeNetwork.lastRequest?.url)
        assertEquals("GET", fakeNetwork.lastRequest?.method)
    }

    @Test
    fun testFetchScreenJsonFailureMapsCleanlyToResultFailure() = runTest {
        val fakeNetwork = FakeNetworkClient { req ->
            NetworkResponse(
                statusCode = 404,
                bodyJson = """{"error":"Not Found"}""",
                isSuccessful = false
            )
        }
        val repository = NetworkSduiScreenRepository(fakeNetwork)

        val result = repository.fetchScreenJson("/api/v1/sdui/invalid")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("HTTP status 404") == true)
    }
}
