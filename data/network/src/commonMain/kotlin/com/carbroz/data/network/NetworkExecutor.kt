package com.carbroz.data.network

/** Transport SPI. Ktor belongs behind this interface, not in runtime:action. */
fun interface NetworkTransport {
    suspend fun execute(request: TransportRequest): NetworkResult
}

data class TransportRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?,
)

class NetworkExecutor(
    private val environment: NetworkEnvironment,
    private val transport: NetworkTransport,
    private val headerProvider: NetworkHeaderProvider = EmptyNetworkHeaderProvider,
    private val headerPolicy: NetworkHeaderPolicy = NetworkHeaderPolicy(),
) {
    suspend fun execute(request: NetworkRequest): NetworkResult {
        val headers = try {
            headerPolicy.merge(headerProvider.headers(), request.headers)
        } catch (error: IllegalArgumentException) {
            return NetworkResult.Failure(NetworkFailure.InvalidRequest(error.message.orEmpty()))
        }

        val transportRequest = TransportRequest(
            method = request.method.name,
            url = environment.resolve(request.endpoint),
            headers = headers,
            body = request.payload.toString(),
        )
        return transport.execute(transportRequest)
    }
}
