package com.carbroz.partner.sdui.host

import com.carbroz.partner.core.navigation.NavCommand
import com.carbroz.partner.core.navigation.NavDestination
import com.carbroz.partner.core.navigation.NavEntry
import com.carbroz.partner.core.navigation.NavResult
import com.carbroz.partner.core.navigation.NavState
import com.carbroz.partner.core.navigation.Router
import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.binding.BindingScope
import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.engine.execution.result.ExecutionResult
import com.carbroz.partner.sdui.host.controller.SduiScreenHostController
import com.carbroz.partner.sdui.host.model.SduiHostState
import com.carbroz.partner.sdui.host.repository.SduiScreenRepository
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SduiScreenHostControllerTest {

    private class FakeRepository(
        private val jsonMap: Map<String, Result<String>>
    ) : SduiScreenRepository {
        var requestedEndpoints = mutableListOf<String>()

        override suspend fun fetchScreenJson(endpoint: String): Result<String> {
            requestedEndpoints.add(endpoint)
            return jsonMap[endpoint] ?: Result.failure(IllegalArgumentException("404 Not Found"))
        }
    }

    private class FakeActionDispatcher : ActionDispatcher {
        override suspend fun dispatch(action: ActionSpec, scope: BindingScope?): ExecutionResult {
            return ExecutionResult.Success()
        }
    }

    private class FakeRouter : Router {
        var popCount = 0

        override val state: StateFlow<NavState>
            get() = error("State is not read by SduiScreenHostController")

        override suspend fun execute(command: NavCommand): NavResult {
            if (command is NavCommand.Pop) {
                popCount++
            }
            return NavResult.Executed(NavEntry("entry_1", NavDestination.create("dummy")))
        }
    }

    private val screenNoBackJson = """
    {
      "schema_version": 1,
      "screen_id": "screen_normal",
      "title": "Normal Screen",
      "show_back": true,
      "template": {
        "template_id": "tpl_1",
        "template_type": "form_template",
        "width": "fill",
        "height": "fill",
        "axis": "vertical",
        "components": [
          {
            "component_id": "cmp_1",
            "component_type": "container",
            "width": "fill",
            "height": "wrap"
          }
        ]
      }
    }
    """.trimIndent()

    private val screenWithServerBackJson = """
    {
      "schema_version": 1,
      "screen_id": "screen_server_back",
      "title": "Server Back Screen",
      "show_back": true,
      "back": {
        "api": "/api/v1/login",
        "template_id": "tpl_login",
        "template_type": "form_template"
      },
      "template": {
        "template_id": "tpl_1",
        "template_type": "form_template",
        "width": "fill",
        "height": "fill",
        "axis": "vertical",
        "components": [
          {
            "component_id": "cmp_1",
            "component_type": "container",
            "width": "fill",
            "height": "wrap"
          }
        ]
      }
    }
    """.trimIndent()

    private val targetBackScreenJson = """
    {
      "schema_version": 1,
      "screen_id": "screen_login",
      "title": "Login Screen",
      "show_back": false,
      "template": {
        "template_id": "tpl_login",
        "template_type": "form_template",
        "width": "fill",
        "height": "fill",
        "axis": "vertical",
        "components": [
          {
            "component_id": "cmp_login",
            "component_type": "container",
            "width": "fill",
            "height": "wrap"
          }
        ]
      }
    }
    """.trimIndent()

    @Test
    fun testNormalBackInvokesRouterPopWithoutNetwork() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val controllerScope = TestScope(testDispatcher)
        val repo = FakeRepository(mapOf("/api/v1/screen" to Result.success(screenNoBackJson)))
        val router = FakeRouter()

        val controller = SduiScreenHostController(
            scope = controllerScope,
            endpoint = "/api/v1/screen",
            repository = repo,
            actionDispatcher = FakeActionDispatcher(),
            router = router
        )

        val content = controller.hostState.first { it is SduiHostState.Content } as SduiHostState.Content
        assertEquals(1, repo.requestedEndpoints.size)

        controller.onUiEvent(SduiUiEvent.BackRequested(content.assembledScreen.screen.back))
        assertEquals(1, router.popCount)
        assertEquals(1, repo.requestedEndpoints.size)
    }

    @Test
    fun testServerDrivenBackLoadsApiAndTransitions() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val controllerScope = TestScope(testDispatcher)
        val repo = FakeRepository(
            mapOf(
                "/api/v1/screen" to Result.success(screenWithServerBackJson),
                "/api/v1/login" to Result.success(targetBackScreenJson)
            )
        )
        val router = FakeRouter()

        val controller = SduiScreenHostController(
            scope = controllerScope,
            endpoint = "/api/v1/screen",
            repository = repo,
            actionDispatcher = FakeActionDispatcher(),
            router = router
        )

        val initialContent = controller.hostState.first { it is SduiHostState.Content } as SduiHostState.Content
        assertEquals("screen_server_back", initialContent.assembledScreen.screen.screenId)

        controller.onUiEvent(SduiUiEvent.BackRequested(initialContent.assembledScreen.screen.back))
        assertEquals(0, router.popCount)
        assertEquals(2, repo.requestedEndpoints.size)
        assertEquals("/api/v1/login", repo.requestedEndpoints[1])

        val updatedContent = controller.hostState.first { it is SduiHostState.Content && (it.assembledScreen.screen.screenId == "screen_login") } as SduiHostState.Content
        assertEquals("screen_login", updatedContent.assembledScreen.screen.screenId)
    }

    @Test
    fun testServerDrivenBackFailurePreservesCurrentScreenAndNoPop() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val controllerScope = TestScope(testDispatcher)
        val repo = FakeRepository(
            mapOf(
                "/api/v1/screen" to Result.success(screenWithServerBackJson)
            )
        )
        val router = FakeRouter()

        val controller = SduiScreenHostController(
            scope = controllerScope,
            endpoint = "/api/v1/screen",
            repository = repo,
            actionDispatcher = FakeActionDispatcher(),
            router = router
        )

        val initialContent = controller.hostState.first { it is SduiHostState.Content } as SduiHostState.Content
        assertEquals("screen_server_back", initialContent.assembledScreen.screen.screenId)

        controller.onUiEvent(SduiUiEvent.BackRequested(initialContent.assembledScreen.screen.back))
        assertEquals(0, router.popCount)

        val finalState = controller.hostState.value
        assertTrue(finalState is SduiHostState.Content)
        val finalContent = finalState as SduiHostState.Content
        assertEquals("screen_server_back", finalContent.assembledScreen.screen.screenId)
        assertFalse(finalContent.isRefreshing)
    }
}
