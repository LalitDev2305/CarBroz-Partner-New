package com.carbroz.feature.splash

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BootstrapCacheResilienceTest {
    private val client = BootstrapClientCapabilities(
        versionName = "1.0.0",
        versionCode = 1,
        applicationId = "com.carbroz.partner",
    )

    @Test
    fun `cache read failure requests full server configuration and startup continues`() = runTest {
        var request: NetworkRequest? = null
        val store = BootstrapStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { captured ->
                request = captured
                successfulResponse()
            },
            store = store,
            configurationCache = ThrowingCache(failRead = true),
            client = client,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        assertIs<BootstrapState.Ready>(store.state.value)
        assertNull(request?.headers?.get("X-CarBroz-Config-Version"))
    }

    @Test
    fun `cache write failure does not block valid fresh startup`() = runTest {
        val store = BootstrapStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { successfulResponse() },
            store = store,
            configurationCache = ThrowingCache(failWrite = true),
            client = client,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        assertIs<BootstrapState.Ready>(store.state.value)
    }

    private fun successfulResponse(): NetworkResult.Success = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("meta", buildJsonObject {
                    put("serverTimeEpochMilliseconds", 1_700_000_000_000L)
                    put("bootstrapSchemaVersion", 1)
                })
                put("config", buildJsonObject {
                    put("changed", true)
                    put("version", "cfg-1")
                    put("data", buildJsonObject { put("enabled", true) })
                })
                put("sdui", buildJsonObject {
                    put("protocolVersion", 1)
                    put("schemaVersion", 1)
                })
                put("nextScreen", buildJsonObject {
                    put("screenId", "entry")
                    put("templateId", "form")
                    put("templateType", "FORM_TEMPLATE")
                    put("endpoint", "/api/v1/screen/entry")
                    put("method", "GET")
                    put("authentication", "OPTIONAL_SESSION")
                    put("transition", "RESET")
                    put("restorePolicy", "CACHE_FIRST")
                    put("backStackKey", "entry")
                })
            },
        ),
    )

    private class ThrowingCache(
        private val failRead: Boolean = false,
        private val failWrite: Boolean = false,
    ) : BootstrapConfigurationCache {
        override suspend fun read(): BootstrapRemoteConfiguration? {
            if (failRead) error("cache read unavailable")
            return null
        }

        override suspend fun write(configuration: BootstrapRemoteConfiguration) {
            if (failWrite) error("cache write unavailable")
        }

        override suspend fun clear() = Unit
    }
}
