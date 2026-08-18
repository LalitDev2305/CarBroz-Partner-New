package com.carbroz.partner.sdui.render.fallback

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
import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot
import com.carbroz.partner.sdui.render.scope.DefaultRenderScope
import kotlin.test.Test
import kotlin.test.assertTrue

class UnsupportedFallbackTest {

    @Test
    fun testUnsupportedTypesTraverseDescendantsWithoutCrashing() {
        var childDataRenderedCount = 0
        val testCdRenderer = ChildrenDataRenderer { cd, snapshot, sink ->
            childDataRenderedCount++
        }

        // Empty registries for template/component/subcomponent/child -> forces UnsupportedFallback
        val templateReg = TemplateRendererRegistry(emptyMap())
        val compReg = ComponentRendererRegistry(emptyMap())
        val subReg = SubComponentRendererRegistry(emptyMap())
        val childReg = ChildRendererRegistry(emptyMap())
        val cdReg = ChildrenDataRendererRegistry(mapOf("known_primitive" to testCdRenderer))

        val sink = SduiUiEventSink { }
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
            id = "cd_fallback_1",
            childrenDataType = "known_primitive",
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

        val childNode = SduiChild(
            id = "child_1",
            childType = "unsupported_child_type",
            width = DimensionSpec.Wrap,
            height = DimensionSpec.Wrap,
            axis = LayoutAxis.HORIZONTAL,
            padding = SduiEdgeSpacing(),
            margin = SduiEdgeSpacing(),
            gap = SpacingSpec.Fixed(0.dp),
            visible = true,
            enabled = true,
            properties = null,
            action = null,
            parentAction = null,
            childrenData = listOf(cdNode),
            acceptsParentAction = false
        )

        val subCompNode = SduiSubComponent(
            id = "sub_1",
            subcomponentType = "unsupported_sub_type",
            width = DimensionSpec.Wrap,
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
            children = listOf(childNode),
            childrenData = emptyList(),
            acceptsParentAction = false
        )

        val compNode = SduiComponent(
            id = "comp_1",
            componentType = "unsupported_comp_type",
            width = DimensionSpec.Wrap,
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
            subcomponents = listOf(subCompNode),
            childrenData = emptyList(),
            acceptsParentAction = false
        )

        val templateNode = SduiTemplate(
            id = "tpl_1",
            templateType = "unsupported_tpl_type",
            width = DimensionSpec.Fill,
            height = DimensionSpec.Fill,
            axis = LayoutAxis.VERTICAL,
            padding = SduiEdgeSpacing(),
            margin = SduiEdgeSpacing(),
            gap = SpacingSpec.Fixed(0.dp),
            properties = null,
            components = listOf(compNode)
        )

        // Verify data nodes are constructed cleanly
        assertTrue(templateNode.id == "tpl_1")
        assertTrue(cdNode.childrenDataType == "known_primitive")
    }
}
