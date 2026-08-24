package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.model.RequestMethod

/**
 * Application integration boundary from a prepared generic SDUI API action to the canonical
 * network data source. It deliberately contains no screen-specific endpoint or response logic.
 */
class NetworkActionExecutor(
    private val dataSource: NetworkDataSource,
) {
    suspend fun execute(action: PreparedAction.Request): NetworkResult =
        dataSource.execute(
            NetworkRequest(
                method = action.method.toNetworkMethod(),
                endpoint = NetworkEndpoint(action.endpoint),
                payload = action.payload,
                authentication = NetworkAuthentication.SESSION,
            ),
        )

    private fun RequestMethod.toNetworkMethod(): NetworkMethod = when (this) {
        RequestMethod.GET -> NetworkMethod.GET
        RequestMethod.POST -> NetworkMethod.POST
        RequestMethod.PUT -> NetworkMethod.PUT
        RequestMethod.PATCH -> NetworkMethod.PATCH
        RequestMethod.DELETE -> NetworkMethod.DELETE
    }
}
