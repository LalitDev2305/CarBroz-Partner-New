package com.carbroz.partner.sdui.runtime

import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.binding.BindingScope
import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.engine.execution.result.ExecutionResult
import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiAction
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing
import com.carbroz.partner.sdui.engine.model.SduiScreen
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.runtime.event.SduiRuntimeEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SduiRuntimeTest {

    private class FakeActionDispatcher(
        private val resultToReturn: ExecutionResult
    ) : ActionDispatcher {
        var lastAction: ActionSpec? = null
        var lastScope: BindingScope? = null

        override suspend fun dispatch(action: ActionSpec, scope: BindingScope?): ExecutionResult {
            lastAction = action
            lastScope = scope
            return resultToReturn
        }
    }

    private val testBtn = SduiChildrenData(
        id = "btn_1",
        childrenDataType = "button",
        width = DimensionSpec.Fill,
        height = DimensionSpec.Wrap,
        padding = SduiEdgeSpacing(),
        margin = SduiEdgeSpacing(),
        visible = true,
        enabled = true,
        properties = null,
        action = SduiAction(api = "/api/v1/send-otp", templateId = "tpl_otp", templateType = "form_template", payload = null),
        parentAction = null,
        acceptsParentAction = false
    )

    private val testScreen = AssembledSduiScreen(
        screen = SduiScreen(
            schemaVersion = 1,
            screenId = "scr_1",
            title = "Test",
            showBack = true,
            back = null,
            refresh = null,
            theme = null,
            template = SduiTemplate(
                id = "tpl_1",
                templateType = "form_template",
                width = DimensionSpec.Fill,
                height = DimensionSpec.Fill,
                axis = LayoutAxis.VERTICAL,
                padding = SduiEdgeSpacing(),
                margin = SduiEdgeSpacing(),
                gap = SpacingSpec.Fixed(0.dp),
                properties = null,
                components = emptyList()
            )
        ),
        nodeIndex = mapOf("btn_1" to testBtn)
    )

    @Test
    fun testValueChangedUpdatesOverlay() = runTest {
        val dispatcher = FakeActionDispatcher(ExecutionResult.Success())
        val runtime = SduiRuntime(testScreen, dispatcher)

        runtime.onEvent(SduiUiEvent.ValueChanged("phone", "9876543210"))
        val currentSnapshot = runtime.state.value.snapshot
        assertEquals("9876543210", currentSnapshot.getNodeValue("phone"))
        assertEquals("9876543210", currentSnapshot.getInputValue("phone"))
    }

    @Test
    fun testInputChangedBackwardCompatibility() = runTest {
        val dispatcher = FakeActionDispatcher(ExecutionResult.Success())
        val runtime = SduiRuntime(testScreen, dispatcher)

        @Suppress("DEPRECATION")
        runtime.onEvent(SduiUiEvent.InputChanged("phone", "9876543210"))
        val currentSnapshot = runtime.state.value.snapshot
        assertEquals("9876543210", currentSnapshot.getNodeValue("phone"))
    }

    @Test
    fun testNodeTriggeredDispatchesAction() = runTest {
        val dispatcher = FakeActionDispatcher(ExecutionResult.Success())
        val runtime = SduiRuntime(testScreen, dispatcher)

        runtime.onEvent(SduiUiEvent.NodeTriggered(testBtn, testBtn.action, null))

        assertEquals(ActionType.API_REQUEST, dispatcher.lastAction?.type)
        val effect = runtime.effects.replayCache.firstOrNull() ?: runtime.effects.first()
        assertTrue(effect is SduiRuntimeEffect.ScreenTransition)
        assertEquals("/api/v1/send-otp", (effect as SduiRuntimeEffect.ScreenTransition).api)
    }
}
