package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.sdui.SduiPipelineResult
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import com.carbroz.runtime.sdui.template.form.runtime.FormTemplateRuntimeFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicFeatureStoreTest {
    @Test
    fun `dynamic runtime processes arbitrary screen through canonical pipeline`() {
        val runtime = createDynamicSduiRuntime(testConfiguration())
        val result = runtime.pipeline.process(VALID_SCREEN)
        val success = assertIs<SduiPipelineResult.Success>(result)
        assertEquals("screen-a", success.screen.id.value)
        assertEquals("FORM_TEMPLATE", success.screen.template.type.value)
    }

    @Test
    fun `dynamic feature validates destination identity and accepts trusted screen`() = runTest {
        val network = QueueNetworkDataSource(
            NetworkResult.Success(NetworkResponse(200, body = Json.parseToJsonElement(VALID_SCREEN))),
        )
        val runtime = createDynamicSduiRuntime(testConfiguration())
        val store = createStore(network, runtime)

        store.show(destination())
        advanceUntilIdle()

        assertNotNull(store.state.value.screen)
        assertEquals("screen-a", store.state.value.screen?.id?.value)
        assertNull(store.state.value.failure)
        store.close()
    }

    @Test
    fun `malformed network screen fails closed at canonical pipeline`() = runTest {
        val network = QueueNetworkDataSource(
            NetworkResult.Success(NetworkResponse(200, body = Json.parseToJsonElement("{\"unexpected\":true}"))),
        )
        val runtime = createDynamicSduiRuntime(testConfiguration())
        val store = createStore(network, runtime)

        store.show(destination())
        advanceUntilIdle()

        assertNull(store.state.value.screen)
        assertIs<DynamicScreenFailure.Protocol>(store.state.value.failure)
        store.close()
    }

    private fun kotlinx.coroutines.test.TestScope.createStore(
        network: NetworkDataSource,
        runtime: DynamicSduiRuntime,
    ): DynamicFeatureStore {
        val root = destination()
        return DynamicFeatureStore(
            scope = this,
            runtime = runtime,
            actionPreparer = runtime.actions,
            networkActions = NetworkActionExecutor(network),
            capabilityActions = CapabilityActionExecutor(CapabilityRegistry.builder().build()),
            navigation = NavigationStore(NavigationState(listOf(root))),
            bindingContexts = DynamicBindingContextFactory { BindingContext.of() },
            formRuntime = FormTemplateRuntimeFactory(runtime.registry),
            backgroundActions = BackgroundActionExecutor(null, null),
        )
    }

    private fun destination() = DynamicDestination(
        DynamicScreenInstruction(
            destination = ScreenDestination("screen-a", "template-a", NodeType("FORM_TEMPLATE")),
            request = DynamicScreenRequest(
                method = RequestMethod.GET,
                endpoint = "/api/v1/screen/a",
                authentication = RequestAuthentication.NONE,
            ),
            transition = ScreenTransition.RESET,
            backStackKey = "a",
        ),
    )

    private class QueueNetworkDataSource(private val result: NetworkResult) : NetworkDataSource {
        override suspend fun execute(request: NetworkRequest): NetworkResult = result
    }

    private companion object {
        fun testConfiguration() = AppConfiguration(
            environment = AppEnvironment.Development,
            apiBaseUrl = "https://development.invalid",
            buildInformation = BuildInformation("1.0", 1, "com.carbroz.test"),
        )

        const val VALID_SCREEN = """
            {
              "protocolVersion": 1,
              "schemaVersion": 1,
              "minimumClientVersion": 1,
              "screen": {
                "id": "screen-a",
                "version": 1,
                "template": {
                  "id": "template-a",
                  "type": "FORM_TEMPLATE",
                  "components": [
                    {
                      "id": "content",
                      "type": "STACK",
                      "elements": [
                        {
                          "id": "title",
                          "type": "TEXT",
                          "properties": { "text": "Dynamic SDUI", "style": "TITLE_LARGE" }
                        }
                      ]
                    }
                  ]
                }
              },
              "requiredDefinitions": ["FORM_TEMPLATE", "STACK", "TEXT"]
            }
        """
    }
}
