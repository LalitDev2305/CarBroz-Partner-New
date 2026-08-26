package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.runtime.action.ActionPreparer
import com.carbroz.runtime.action.ActionRegistry
import com.carbroz.runtime.action.CoreActionDefinitions
import com.carbroz.runtime.sdui.SduiPipelineResult
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.rendering.SduiRenderFailure
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
class ReferenceSduiStoreTest {
    @Test
    fun `reference runtime processes neutral core screen through canonical pipeline`() {
        val runtime = createReferenceSduiRuntime(testConfiguration())

        val result = runtime.pipeline.process(VALID_SCREEN)

        val success = assertIs<SduiPipelineResult.Success>(result)
        assertEquals("reference", success.screen.id.value)
        assertEquals("FORM_TEMPLATE", success.screen.template.type.value)
    }

    @Test
    fun `render failure invalidates current screen instead of remaining hidden`() = runTest {
        val runtime = createReferenceSduiRuntime(testConfiguration())
        val network = QueueNetworkDataSource(
            NetworkResult.Success(
                NetworkResponse(
                    statusCode = 200,
                    body = Json.parseToJsonElement(VALID_SCREEN),
                ),
            ),
        )
        val store = ReferenceSduiStore(
            network = network,
            pipeline = runtime.pipeline,
            actionPreparer = ActionPreparer(
                ActionRegistry.builder().registerAll(CoreActionDefinitions.all).build(),
            ),
            networkActions = NetworkActionExecutor(network),
            capabilityActions = CapabilityActionExecutor(createCapabilityRegistry(emptyList())),
            parentScope = this,
        )

        try {
            store.dispatch(ReferenceSduiIntent.Load)
            advanceUntilIdle()
            val screen = assertNotNull(store.state.value.screen)

            val failure = SduiRenderFailure.MissingDefinition(
                kind = NodeKind.TEMPLATE,
                type = NodeType("UNKNOWN"),
                path = screen.template.path,
            )
            store.dispatch(ReferenceSduiIntent.RenderFailed(failure))
            advanceUntilIdle()

            assertNull(store.state.value.screen)
            assertEquals(
                ReferenceSduiFailure.Render(failure),
                store.state.value.failure,
            )
        } finally {
            store.close()
        }
    }

    @Test
    fun `malformed network screen fails closed at SDUI pipeline`() = runTest {
        val runtime = createReferenceSduiRuntime(testConfiguration())
        val network = QueueNetworkDataSource(
            NetworkResult.Success(
                NetworkResponse(
                    statusCode = 200,
                    body = Json.parseToJsonElement("{\"unexpected\":true}"),
                ),
            ),
        )
        val store = ReferenceSduiStore(
            network = network,
            pipeline = runtime.pipeline,
            actionPreparer = runtime.actions,
            networkActions = NetworkActionExecutor(network),
            capabilityActions = CapabilityActionExecutor(createCapabilityRegistry(emptyList())),
            parentScope = this,
        )

        try {
            store.dispatch(ReferenceSduiIntent.Load)
            advanceUntilIdle()

            assertNull(store.state.value.screen)
            assertIs<ReferenceSduiFailure.Pipeline>(store.state.value.failure)
        } finally {
            store.close()
        }
    }

    private class QueueNetworkDataSource(
        private val result: NetworkResult,
    ) : NetworkDataSource {
        override suspend fun execute(request: NetworkRequest): NetworkResult = result
    }

    private companion object {
        fun testConfiguration() = AppConfiguration(
            environment = AppEnvironment.Development,
            apiBaseUrl = "https://development.invalid",
            buildInformation = BuildInformation(
                versionName = "1.0",
                versionCode = 1,
                applicationId = "com.carbroz.test",
            ),
        )

        const val VALID_SCREEN = """
            {
              "protocolVersion": 1,
              "schemaVersion": 1,
              "minimumClientVersion": 1,
              "screen": {
                "id": "reference",
                "version": 1,
                "template": {
                  "id": "reference-template",
                  "type": "FORM_TEMPLATE",
                  "components": [
                    {
                      "id": "content",
                      "type": "STACK",
                      "elements": [
                        {
                          "id": "title",
                          "type": "TEXT",
                          "properties": {
                            "text": "Reference SDUI",
                            "style": "TITLE_LARGE"
                          }
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
