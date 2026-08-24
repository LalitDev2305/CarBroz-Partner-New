package com.carbroz.data.network

/**
 * Canonical data-source boundary for callers that need backend I/O without depending on
 * transport, retry, authentication, cache or observability implementation details.
 */
fun interface NetworkDataSource {
    suspend fun execute(request: NetworkRequest): NetworkResult
}

class ExecutorNetworkDataSource(
    private val executor: NetworkExecutor,
) : NetworkDataSource {
    override suspend fun execute(request: NetworkRequest): NetworkResult = executor.execute(request)
}
