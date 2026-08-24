package com.carbroz.data.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkConnectivityTest {
    @Test
    fun confirmedOfflineReturnsOfflineWithoutCallingTransport() = runTest {
        var transportCalls = 0
        val events = mutableListOf<NetworkObservation>()
        val executor = executor(
            transport = NetworkTransport {
                transportCalls += 1
                NetworkResult.Success(NetworkResponse(200))
            },
            connectivityProvider = NetworkConnectivityProvider { NetworkConnectivity.OFFLINE },
            observer = NetworkObserver(events::add),
        )

        val result = executor.execute(getRequest())

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Offline, failure.error)
        assertEquals(0, transportCalls)
        assertEquals(
            listOf(
                NetworkObservation.Started(context()),
                NetworkObservation.Finished(context(), NetworkOutcome.Offline),
            ),
            events,
        )
    }

    @Test
    fun unknownConnectivityFailsOpenToTransport() = runTest {
        var transportCalls = 0
        val executor = executor(
            transport = NetworkTransport {
                transportCalls += 1
                NetworkResult.Success(NetworkResponse(200))
            },
            connectivityProvider = NetworkConnectivityProvider { NetworkConnectivity.UNKNOWN },
        )

        assertIs<NetworkResult.Success>(executor.execute(getRequest()))
        assertEquals(1, transportCalls)
    }

    @Test
    fun onlineConnectivityAllowsTransport() = runTest {
        var transportCalls = 0
        val executor = executor(
            transport = NetworkTransport {
                transportCalls += 1
                NetworkResult.Success(NetworkResponse(200))
            },
            connectivityProvider = NetworkConnectivityProvider { NetworkConnectivity.ONLINE },
        )

        assertIs<NetworkResult.Success>(executor.execute(getRequest()))
        assertEquals(1, transportCalls)
    }

    @Test
    fun retryStopsWhenConnectivityBecomesOffline() = runTest {
        var connectivityChecks = 0
        var transportCalls = 0
        var delays = 0
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport {
                transportCalls += 1
                NetworkResult.Failure(NetworkFailure.Transport("temporary"))
            },
            connectivityProvider = NetworkConnectivityProvider {
                connectivityChecks += 1
                if (connectivityChecks == 1) NetworkConnectivity.ONLINE else NetworkConnectivity.OFFLINE
            },
            retryDelay = NetworkRetryDelay { delays += 1 },
        )

        val result = executor.execute(
            getRequest(
                executionPolicy = NetworkExecutionPolicy(
                    maxAttempts = 3,
                    initialRetryDelayMillis = 1,
                ),
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Offline, failure.error)
        assertEquals(1, transportCalls)
        assertEquals(1, delays)
        assertEquals(2, connectivityChecks)
    }

    @Test
    fun offlineFailureDoesNotTriggerAuthenticationRecovery() = runTest {
        var recoveryCalls = 0
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { NetworkResult.Success(NetworkResponse(200)) },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveryCalls += 1
                true
            },
            connectivityProvider = NetworkConnectivityProvider { NetworkConnectivity.OFFLINE },
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/profile"),
                authentication = NetworkAuthentication.SESSION,
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Offline, failure.error)
        assertEquals(0, recoveryCalls)
    }

    private fun executor(
        transport: NetworkTransport,
        connectivityProvider: NetworkConnectivityProvider,
        observer: NetworkObserver = NoNetworkObserver,
    ) = NetworkExecutor(
        environment = NetworkEnvironment("https://api.carbroz.example"),
        transport = transport,
        connectivityProvider = connectivityProvider,
        requestIdProvider = NetworkRequestIdProvider { NetworkRequestId("request-1") },
        observer = observer,
    )

    private fun getRequest(
        executionPolicy: NetworkExecutionPolicy = NetworkExecutionPolicy(),
    ) = NetworkRequest(
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/health"),
        executionPolicy = executionPolicy,
    )

    private fun context() = NetworkRequestContext(
        requestId = NetworkRequestId("request-1"),
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/health"),
    )
}
