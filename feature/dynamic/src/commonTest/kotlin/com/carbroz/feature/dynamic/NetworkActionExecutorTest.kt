package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NetworkActionExecutorTest {
    @Test
    fun preparedRequestUsesItsTrustedAuthenticationPolicy() = runTest {
        val expected = NetworkResult.Success(NetworkResponse(statusCode = 204))
        val dataSource = CapturingNetworkDataSource(expected)
        val executor = NetworkActionExecutor(dataSource)
        val payload = buildJsonObject { put("value", JsonPrimitive("42")) }
        val action = PreparedAction.Request(
            method = RequestMethod.POST,
            endpoint = "/v1/actions/generic",
            destination = null,
            payload = payload,
            authentication = RequestAuthentication.OPTIONAL_SESSION,
            responseMode = RequestResponseMode.NONE,
        )

        val result = executor.execute(action)

        assertSame(expected, result)
        assertEquals(NetworkMethod.POST, dataSource.request?.method)
        assertEquals("/v1/actions/generic", dataSource.request?.endpoint?.value)
        assertEquals(payload, dataSource.request?.payload)
        assertEquals(NetworkAuthentication.OPTIONAL_SESSION, dataSource.request?.authentication)
    }

    @Test
    fun allSemanticMethodsMapWithoutScreenSpecificClients() = runTest {
        RequestMethod.entries.forEach { method ->
            val dataSource = CapturingNetworkDataSource(NetworkResult.Success(NetworkResponse(200)))
            NetworkActionExecutor(dataSource).execute(
                PreparedAction.Request(
                    method = method,
                    endpoint = "/v1/generic",
                    destination = null,
                    payload = buildJsonObject {},
                    responseMode = RequestResponseMode.NONE,
                ),
            )
            assertEquals(method.name, dataSource.request?.method?.name)
        }
    }

    private class CapturingNetworkDataSource(private val result: NetworkResult) : NetworkDataSource {
        var request: NetworkRequest? = null
        override suspend fun execute(request: NetworkRequest): NetworkResult {
            this.request = request
            return result
        }
    }
}
