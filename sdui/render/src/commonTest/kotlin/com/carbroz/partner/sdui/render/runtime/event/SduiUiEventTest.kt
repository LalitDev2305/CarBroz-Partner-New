package com.carbroz.partner.sdui.render.runtime.event

import com.carbroz.partner.sdui.engine.model.SduiAction
import com.carbroz.partner.sdui.engine.model.SduiBackPolicy
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiParentAction
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SduiUiEventTest {

    @Test
    fun testNodeTriggeredEventStructure() {
        val node = SduiChildrenData(
            id = "btn_1",
            childrenDataType = "button",
            width = DimensionSpec.Wrap,
            height = DimensionSpec.Wrap,
            padding = SduiEdgeSpacing(),
            margin = SduiEdgeSpacing(),
            visible = true,
            enabled = true,
            properties = null,
            action = SduiAction("/api/v1/submit", "tpl_1", "form", null),
            parentAction = SduiParentAction("target_123"),
            acceptsParentAction = false
        )

        val event = SduiUiEvent.NodeTriggered(
            node = node,
            action = node.action,
            parentAction = node.parentAction
        )

        assertEquals("btn_1", event.node.id)
        assertEquals("/api/v1/submit", event.action?.api)
        assertEquals("target_123", event.parentAction?.targetId)
    }

    @Test
    fun testInputChangedEventStructure() {
        val event = SduiUiEvent.InputChanged("input_name", "John Doe")
        assertEquals("input_name", event.nodeId)
        assertEquals("John Doe", event.newValue)
    }

    @Test
    fun testBackAndRefreshRequestedEvents() {
        val backPolicy = SduiBackPolicy("/api/v1/back", "tpl_back", "form")
        val backEvent = SduiUiEvent.BackRequested(backPolicy)
        val refreshEvent = SduiUiEvent.RefreshRequested(null)

        assertEquals(backPolicy, backEvent.policy)
        assertEquals(null, refreshEvent.policy)
    }
}
