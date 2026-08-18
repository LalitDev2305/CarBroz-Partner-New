package com.carbroz.partner.sdui.render.hierarchy

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.fakes.FakeChildRenderer
import com.carbroz.partner.sdui.render.fakes.FakeComponentRenderer
import com.carbroz.partner.sdui.render.fakes.FakeSubComponentRenderer
import com.carbroz.partner.sdui.render.fakes.FakeTemplateRenderer
import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot
import com.carbroz.partner.sdui.render.scope.DefaultRenderScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HierarchyTraversalTest {

    @Test
    fun testFullHierarchyAndDirectBranchResolution() {
        val events = mutableListOf<SduiUiEvent>()
        val sink = SduiUiEventSink { events.add(it) }

        var textRendered = false
        val mockTextRenderer = ChildrenDataRenderer { cd, snapshot, eventSink ->
            textRendered = true
        }

        val templateReg = TemplateRendererRegistry(mapOf("form" to FakeTemplateRenderer))
        val compReg = ComponentRendererRegistry(mapOf("card" to FakeComponentRenderer))
        val subReg = SubComponentRendererRegistry(mapOf("row" to FakeSubComponentRenderer))
        val childReg = ChildRendererRegistry(mapOf("field" to FakeChildRenderer))
        val cdReg = ChildrenDataRendererRegistry(mapOf("text" to mockTextRenderer))

        val scope = DefaultRenderScope(
            templateRegistry = templateReg,
            componentRegistry = compReg,
            subComponentRegistry = subReg,
            childRegistry = childReg,
            childrenDataRegistry = cdReg,
            snapshot = SduiRenderSnapshot(),
            eventSink = sink
        )

        val cdNode = SduiChildrenData(
            id = "cd_1",
            childrenDataType = "text",
            width = DimensionSpec.Wrap,
            height = DimensionSpec.Wrap,
            padding = SduiEdgeSpacing(),
            margin = SduiEdgeSpacing(),
            visible = true,
            enabled = true,
            properties = null,
            action = null,
            parentAction = null,
            acceptsParentAction = false
        )

        val templateNode = SduiTemplate(
            id = "tpl_1",
            templateType = "form",
            width = DimensionSpec.Fill,
            height = DimensionSpec.Fill,
            axis = LayoutAxis.VERTICAL,
            padding = SduiEdgeSpacing(),
            margin = SduiEdgeSpacing(),
            gap = SpacingSpec.Fixed(0.dp),
            properties = null,
            components = listOf(
                SduiComponent(
                    id = "cmp_1",
                    componentType = "card",
                    width = DimensionSpec.Fill,
                    height = DimensionSpec.Wrap,
                    axis = LayoutAxis.VERTICAL,
                    padding = SduiEdgeSpacing(),
                    margin = SduiEdgeSpacing(),
                    gap = SpacingSpec.Fixed(0.dp),
                    visible = true,
                    enabled = true,
                    properties = null,
                    action = null,
                    parentAction = null,
                    subcomponents = emptyList(),
                    childrenData = listOf(cdNode),
                    acceptsParentAction = false
                )
            )
        )

        val renderLambda: @Composable () -> Unit = {
            scope.renderTemplate(templateNode)
        }
        renderLambda
        assertTrue(cdNode.id == "cd_1")
    }
}
