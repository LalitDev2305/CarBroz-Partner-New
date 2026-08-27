package com.carbroz.feature.splash

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.feature.dynamic.DynamicRestorePolicy
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTaskResult
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BootstrapConfigurationTest {
    private val client = BootstrapClientCapabilities(
        versionName = "1.2.3",
        versionCode = 123,
        applicationId = "com.carbroz.partner",
    )

    @Test
    fun `successful bootstrap caches config and resolves validated dynamic screen instruction`() = runTest {
        var capturedRequest: NetworkRequest? = null
        val store = BootstrapStore()
        val cache = InMemoryBootstrapCache()
        val task = task(
            network = NetworkDataSource { request ->
                capturedRequest = request
                successfulBootstrapResponse()
            },
            store = store,
            cache = cache,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        val ready = assertIs<BootstrapState.Ready>(store.state.value)
        assertEquals("screen-entry", ready.instruction.destination.screenId)
        assertEquals("template-form", ready.instruction.destination.templateId)
        assertEquals(NodeType("FORM_TEMPLATE"), ready.instruction.destination.templateType)
        assertEquals(RequestMethod.GET, ready.instruction.request.method)
        assertEquals("/api/v1/screen/entry", ready.instruction.request.endpoint)
        assertEquals(RequestAuthentication.OPTIONAL_SESSION, ready.instruction.request.authentication)
        assertEquals(ScreenTransition.RESET, ready.instruction.transition)
        assertEquals(DynamicRestorePolicy.CACHE_FIRST, ready.instruction.restorePolicy)
        assertEquals("entry", ready.instruction.backStackKey)
        assertEquals("cfg-2", cache.value?.version)
        assertEquals("enabled", cache.value?.data?.get("partnerMode")?.let { (it as JsonPrimitive).content })
        assertEquals(NetworkMethod.GET, capturedRequest?.method)
        assertEquals("/api/v1/app", capturedRequest?.endpoint?.value)
        assertEquals(NetworkAuthentication.OPTIONAL_SESSION, capturedRequest?.authentication)
        assertEquals("1.2.3", capturedRequest?.headers?.get("X-CarBroz-App-Version"))
        assertEquals("123", capturedRequest?.headers?.get("X-CarBroz-Build-Number"))
    }

    @Test
    fun `unchanged config uses matching cached config while startup context stays fresh`() = runTest {
        val cache = InMemoryBootstrapCache(
            BootstrapRemoteConfiguration("cfg-1", buildJsonObject { put("cached", true) }),
        )
        var capturedRequest: NetworkRequest? = null
        val store = BootstrapStore()
        val task = task(
            network = NetworkDataSource { request ->
                capturedRequest = request
                successfulBootstrapResponse(
                    configChanged = false,
                    configVersion = "cfg-1",
                    configData = null,
                    userName = "Fresh User",
                )
            },
            store = store,
            cache = cache,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        val ready = assertIs<BootstrapState.Ready>(store.state.value)
        assertEquals("cfg-1", ready.snapshot.configuration.version)
        assertEquals("Fresh User", ready.snapshot.user?.displayName)
        assertEquals("cfg-1", capturedRequest?.headers?.get("X-CarBroz-Config-Version"))
    }

    @Test
    fun `required update blocks dynamic destination`() = runTest {
        val store = BootstrapStore()
        val task = task(
            network = NetworkDataSource {
                successfulBootstrapResponse(
                    updateMode = "REQUIRED",
                    storeUrl = "https://play.google.com/store/apps/details?id=com.carbroz.partner",
                    includeNextScreen = false,
                )
            },
            store = store,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        val blocked = assertIs<BootstrapState.ForceUpdate>(store.state.value)
        assertEquals(BootstrapUpdateMode.REQUIRED, blocked.policy.mode)
        assertNull(store.currentInstruction())
    }

    @Test
    fun `maintenance blocks dynamic destination`() = runTest {
        val store = BootstrapStore()
        val task = task(
            network = NetworkDataSource {
                successfulBootstrapResponse(maintenance = true, includeNextScreen = false)
            },
            store = store,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        assertIs<BootstrapState.Maintenance>(store.state.value)
        assertNull(store.currentInstruction())
    }

    @Test
    fun `server cannot claim unchanged config without matching local version`() = runTest {
        val store = BootstrapStore()
        val task = task(
            network = NetworkDataSource {
                successfulBootstrapResponse(configChanged = false, configVersion = "missing", configData = null)
            },
            store = store,
            cache = InMemoryBootstrapCache(),
        )

        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_config_cache_miss", reason.code)
        assertNull(store.currentInstruction())
    }

    @Test
    fun `invalid dynamic screen instruction is rejected without publishing destination`() = runTest {
        val store = BootstrapStore()
        val task = task(
            network = NetworkDataSource {
                successfulBootstrapResponse(endpoint = "https://untrusted.example/screen")
            },
            store = store,
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_dynamic_instruction_invalid_endpoint", reason.code)
        assertNull(store.currentInstruction())
    }

    @Test
    fun `unsupported bootstrap schema fails closed`() = runTest {
        val task = task(
            network = NetworkDataSource { successfulBootstrapResponse(bootstrapSchema = 9) },
            store = BootstrapStore(),
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_schema_unsupported", reason.code)
        assertEquals(false, reason.recoverable)
    }

    @Test
    fun `offline bootstrap is recoverable`() = runTest {
        val task = task(
            network = NetworkDataSource { NetworkResult.Failure(NetworkFailure.Offline) },
            store = BootstrapStore(),
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_offline", reason.code)
        assertEquals(true, reason.recoverable)
    }

    @Test
    fun `malformed successful response fails closed`() = runTest {
        val store = BootstrapStore()
        val task = task(
            network = NetworkDataSource {
                NetworkResult.Success(NetworkResponse(statusCode = 200, body = JsonPrimitive("not-an-object")))
            },
            store = store,
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_invalid_payload", reason.code)
        assertNull(store.currentInstruction())
    }

    private fun task(
        network: NetworkDataSource,
        store: BootstrapStore,
        cache: BootstrapConfigurationCache = InMemoryBootstrapCache(),
    ) = BootstrapConfigurationStartupTask(
        network = network,
        store = store,
        configurationCache = cache,
        client = client,
    )

    private fun successfulBootstrapResponse(
        endpoint: String = "/api/v1/screen/entry",
        configChanged: Boolean = true,
        configVersion: String = "cfg-2",
        configData: JsonObject? = buildJsonObject { put("partnerMode", "enabled") },
        updateMode: String = "NONE",
        storeUrl: String? = null,
        maintenance: Boolean = false,
        includeNextScreen: Boolean = true,
        userName: String = "Partner",
        bootstrapSchema: Int = 1,
    ): NetworkResult.Success = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("meta", buildJsonObject {
                    put("requestId", "request-1")
                    put("serverTimeEpochMilliseconds", 1_700_000_000_000L)
                    put("bootstrapSchemaVersion", bootstrapSchema)
                })
                put("config", buildJsonObject {
                    put("changed", configChanged)
                    put("version", configVersion)
                    if (configData != null) put("data", configData)
                })
                put("updatePolicy", buildJsonObject {
                    put("mode", updateMode)
                    put("title", "Update CarBroz Partner")
                    if (storeUrl != null) put("storeUrl", storeUrl)
                })
                put("maintenance", buildJsonObject {
                    put("enabled", maintenance)
                    put("title", "Scheduled maintenance")
                })
                put("session", buildJsonObject { put("authenticated", true) })
                put("user", buildJsonObject {
                    put("id", "user-1")
                    put("displayName", userName)
                })
                put("partner", buildJsonObject {
                    put("partnerId", "partner-1")
                    put("accountStatus", "ACTIVE")
                })
                put("sdui", buildJsonObject {
                    put("protocolVersion", 1)
                    put("schemaVersion", 1)
                })
                if (includeNextScreen) {
                    put("nextScreen", buildJsonObject {
                        put("screenId", "screen-entry")
                        put("templateId", "template-form")
                        put("templateType", "FORM_TEMPLATE")
                        put("endpoint", endpoint)
                        put("method", "GET")
                        put("authentication", "OPTIONAL_SESSION")
                        put("transition", "RESET")
                        put("restorePolicy", "CACHE_FIRST")
                        put("backStackKey", "entry")
                    })
                }
            },
        ),
    )

    private class InMemoryBootstrapCache(
        var value: BootstrapRemoteConfiguration? = null,
    ) : BootstrapConfigurationCache {
        override suspend fun read(): BootstrapRemoteConfiguration? = value
        override suspend fun write(configuration: BootstrapRemoteConfiguration) { value = configuration }
        override suspend fun clear() { value = null }
    }
}
