package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkCachePolicy
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Semantic result of the application bootstrap/config response. */
enum class BootstrapRoute {
    Reference,
}

/**
 * Process-scoped owner of the route selected by the bootstrap/config response.
 *
 * It deliberately stores a semantic route instead of Navigation 3/framework state.
 */
class BootstrapRouteStore {
    private var resolvedRoute: BootstrapRoute? = null

    fun resolve(route: BootstrapRoute) {
        resolvedRoute = route
    }

    fun current(): BootstrapRoute? = resolvedRoute
}

/** Maps a resolved bootstrap route to an application semantic destination. */
fun BootstrapRoute.toNavigationDestination(): NavigationDestination = when (this) {
    BootstrapRoute.Reference -> ReferenceDestination
}

/**
 * Executes the real application bootstrap/config request through the canonical network stack.
 *
 * The endpoint is relative, unauthenticated and explicitly cacheable. Successful responses must
 * provide an allow-listed semantic `nextDestination`; arbitrary server navigation identifiers are
 * never executed directly.
 */
class BootstrapConfigurationStartupTask(
    private val network: NetworkDataSource,
    private val routes: BootstrapRouteStore,
) : StartupTask {
    override val id: String = "bootstrap-configuration"

    override suspend fun execute(): StartupTaskResult {
        return when (
            val result = network.execute(
                NetworkRequest(
                    method = NetworkMethod.GET,
                    endpoint = NetworkEndpoint("/api/v1/app"),
                    authentication = NetworkAuthentication.NONE,
                    cachePolicy = NetworkCachePolicy.NetworkFirst(
                        fallbackMaxAgeMillis = BOOTSTRAP_CACHE_MAX_AGE_MILLIS,
                    ),
                ),
            )
        ) {
            is NetworkResult.Success -> handleSuccess(result)
            is NetworkResult.Failure -> StartupTaskResult.Failure(result.error.toStartupFailure())
        }
    }

    private fun handleSuccess(result: NetworkResult.Success): StartupTaskResult {
        val response = result.response
        if (response.statusCode !in 200..299) {
            return StartupTaskResult.Failure(
                StartupFailure.Expected(
                    code = "bootstrap_http_${response.statusCode}",
                    recoverable = response.statusCode >= 500,
                ),
            )
        }

        val body = response.body as? JsonObject
            ?: return invalidResponse("bootstrap_missing_object")
        val destination = body["nextDestination"]?.jsonPrimitive?.contentOrNull
            ?: return invalidResponse("bootstrap_missing_destination")

        val route = when (destination.trim().lowercase()) {
            "reference", "reference-sdui" -> BootstrapRoute.Reference
            else -> return invalidResponse("bootstrap_unsupported_destination")
        }

        routes.resolve(route)
        return StartupTaskResult.Success
    }

    private fun invalidResponse(code: String): StartupTaskResult.Failure =
        StartupTaskResult.Failure(
            StartupFailure.Expected(
                code = code,
                recoverable = false,
            ),
        )

    private fun NetworkFailure.toStartupFailure(): StartupFailure.Expected = when (this) {
        NetworkFailure.Offline -> StartupFailure.Expected("bootstrap_offline", recoverable = true)
        NetworkFailure.Timeout -> StartupFailure.Expected("bootstrap_timeout", recoverable = true)
        is NetworkFailure.Http -> StartupFailure.Expected(
            code = "bootstrap_http_$statusCode",
            recoverable = statusCode >= 500 || statusCode == 408 || statusCode == 429,
        )
        NetworkFailure.Transport -> StartupFailure.Expected("bootstrap_transport", recoverable = true)
        is NetworkFailure.InvalidRequest -> StartupFailure.Expected(
            code = "bootstrap_invalid_request",
            recoverable = false,
        )
    }

    private companion object {
        const val BOOTSTRAP_CACHE_MAX_AGE_MILLIS: Long = 5 * 60 * 1000L
    }
}
