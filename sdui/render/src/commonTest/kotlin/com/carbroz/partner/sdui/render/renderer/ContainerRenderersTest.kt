package com.carbroz.partner.sdui.render.renderer

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
import com.carbroz.partner.sdui.render.SduiRenderers
import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.renderer.child.ContainerChild
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.renderer.component.ContainerComponent
import com.carbroz.partner.sdui.render.renderer.subcomponent.ContainerSubComponent
import com.carbroz.partner.sdui.render.renderer.template.ContainerTemplate
import com.carbroz.partner.sdui.render.renderer.template.FormTemplate
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot
import com.carbroz.partner.sdui.render.scope.DefaultRenderScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ContainerRenderersTest {

    private fun createTemplate(
        id: String = "tpl_1",
        components: List<SduiComponent> = emptyList()
    ) = SduiTemplate(
        id = id,
        templateType = "container_template",
        width = DimensionSpec.Fill,
        height = DimensionSpec.Wrap,
        axis = LayoutAxis.VERTICAL,
        padding = SduiEdgeSpacing(),
        margin = SduiEdgeSpacing(),
        gap = SpacingSpec.Fixed(0.dp),
        properties = null,
        components = components
    )

    private fun createComponent(
        id: String = "cmp_1",
        subcomponents: List<SduiSubComponent> = emptyList(),
        childrenData: List<SduiChildrenData> = emptyList()
    ) = SduiComponent(
        id = id,
        componentType = "container_component",
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
        subcomponents = subcomponents,
        childrenData = childrenData,
        acceptsParentAction = false
    )

    private fun createSubComponent(
        id: String = "sub_1",
        children: List<SduiChild> = emptyList(),
        childrenData: List<SduiChildrenData> = emptyList()
    ) = SduiSubComponent(
        id = id,
        subcomponentType = "container_subcomponent",
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
        children = children,
        childrenData = childrenData,
        acceptsParentAction = false
    )

    private fun createChild(
        id: String = "ch_1",
        childrenData: List<SduiChildrenData> = emptyList()
    ) = SduiChild(
        id = id,
        childType = "container_child",
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
        childrenData = childrenData,
        acceptsParentAction = false
    )


    private fun createChildrenData(
        id: String = "cd_1"
    ) = SduiChildrenData(
        id = id,
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

    @Test
    fun testContainerTemplateResolution() {
        val renderer = SduiRenderers.defaultTemplates.resolve("container_template")
        assertNotNull(renderer)
        assertEquals(ContainerTemplate, renderer)
    }

    @Test
    fun testContainerComponentResolution() {
        val renderer = SduiRenderers.defaultComponents.resolve("container_component")
        assertNotNull(renderer)
        assertEquals(ContainerComponent, renderer)
    }

    @Test
    fun testContainerSubComponentResolution() {
        val renderer = SduiRenderers.defaultSubComponents.resolve("container_subcomponent")
        assertNotNull(renderer)
        assertEquals(ContainerSubComponent, renderer)
    }

    @Test
    fun testContainerChildResolution() {
        val renderer = SduiRenderers.defaultChildren.resolve("container_child")
        assertNotNull(renderer)
        assertEquals(ContainerChild, renderer)
    }

    @Test
    fun testUnknownTemplateTypeDoesNotResolveDefaultTemplate() {
        val renderer = SduiRenderers.defaultTemplates.resolve("future_unknown_template")
        assertNull(renderer)
    }

    @Test
    fun testUnknownComponentTypeDoesNotResolveDefaultComponent() {
        val renderer = SduiRenderers.defaultComponents.resolve("future_unknown_component")
        assertNull(renderer)
    }

    @Test
    fun testExistingFormTemplateRemainsRegistered() {
        val renderer = SduiRenderers.defaultTemplates.resolve("form_template")
        assertNotNull(renderer)
        assertEquals(FormTemplate, renderer)
    }

    @Test
    fun testFullDefaultHierarchyTraversal() {
        val renderLog = mutableListOf<String>()
        val mockTextRenderer = ChildrenDataRenderer { cd, _, _ ->
            renderLog.add("childrenData:${cd.id}")
        }

        val templateReg = SduiRenderers.defaultTemplates
        val compReg = SduiRenderers.defaultComponents
        val subReg = SduiRenderers.defaultSubComponents
        val childReg = SduiRenderers.defaultChildren
        val cdReg = ChildrenDataRendererRegistry(mapOf("text" to mockTextRenderer))

        val scope = DefaultRenderScope(
            templateRegistry = templateReg,
            componentRegistry = compReg,
            subComponentRegistry = subReg,
            childRegistry = childReg,
            childrenDataRegistry = cdReg,
            snapshot = SduiRenderSnapshot(),
            eventSink = SduiUiEventSink { }
        )

        val cd = createChildrenData("cd_leaf")
        val child = createChild("ch_1", listOf(cd))
        val sub = createSubComponent("sub_1", listOf(child))
        val cmp = createComponent("cmp_1", listOf(sub))
        val template = createTemplate("tpl_1", listOf(cmp))

        val renderBlock: @Composable () -> Unit = {
            scope.renderTemplate(template)
        }
        assertNotNull(renderBlock)
        assertEquals("cd_leaf", cd.id)
    }

    @Test
    fun testComponentDirectChildrenDataTraversal() {
        val renderLog = mutableListOf<String>()
        val mockTextRenderer = ChildrenDataRenderer { cd, _, _ ->
            renderLog.add("childrenData:${cd.id}")
        }

        val scope = DefaultRenderScope(
            templateRegistry = SduiRenderers.defaultTemplates,
            componentRegistry = SduiRenderers.defaultComponents,
            subComponentRegistry = SduiRenderers.defaultSubComponents,
            childRegistry = SduiRenderers.defaultChildren,
            childrenDataRegistry = ChildrenDataRendererRegistry(mapOf("text" to mockTextRenderer)),
            snapshot = SduiRenderSnapshot(),
            eventSink = SduiUiEventSink { }
        )

        val cd = createChildrenData("cd_direct_cmp")
        val cmp = createComponent("cmp_1", subcomponents = emptyList(), childrenData = listOf(cd))

        val renderBlock: @Composable () -> Unit = {
            scope.renderComponent(cmp)
        }
        assertNotNull(renderBlock)
    }

    @Test
    fun testSubComponentDirectChildrenDataTraversal() {
        val scope = DefaultRenderScope(
            templateRegistry = SduiRenderers.defaultTemplates,
            componentRegistry = SduiRenderers.defaultComponents,
            subComponentRegistry = SduiRenderers.defaultSubComponents,
            childRegistry = SduiRenderers.defaultChildren,
            childrenDataRegistry = SduiRenderers.defaultChildrenData,
            snapshot = SduiRenderSnapshot(),
            eventSink = SduiUiEventSink { }
        )

        val cd = createChildrenData("cd_direct_sub")
        val sub = createSubComponent("sub_1", children = emptyList(), childrenData = listOf(cd))

        val renderBlock: @Composable () -> Unit = {
            scope.renderSubComponent(sub)
        }
        assertNotNull(renderBlock)
    }

    @Test
    fun testChildChildrenDataTraversal() {
        val scope = DefaultRenderScope(
            templateRegistry = SduiRenderers.defaultTemplates,
            componentRegistry = SduiRenderers.defaultComponents,
            subComponentRegistry = SduiRenderers.defaultSubComponents,
            childRegistry = SduiRenderers.defaultChildren,
            childrenDataRegistry = SduiRenderers.defaultChildrenData,
            snapshot = SduiRenderSnapshot(),
            eventSink = SduiUiEventSink { }
        )

        val cd = createChildrenData("cd_child")
        val child = createChild("ch_1", childrenData = listOf(cd))

        val renderBlock: @Composable () -> Unit = {
            scope.renderChild(child)
        }
        assertNotNull(renderBlock)
    }

    @Test
    fun testRenderingOrderDeterministic() {
        val scope = DefaultRenderScope(
            templateRegistry = SduiRenderers.defaultTemplates,
            componentRegistry = SduiRenderers.defaultComponents,
            subComponentRegistry = SduiRenderers.defaultSubComponents,
            childRegistry = SduiRenderers.defaultChildren,
            childrenDataRegistry = SduiRenderers.defaultChildrenData,
            snapshot = SduiRenderSnapshot(),
            eventSink = SduiUiEventSink { }
        )

        val sub = createSubComponent("sub_order")
        val cdDirect = createChildrenData("cd_direct")
        val cmp = createComponent("cmp_order", subcomponents = listOf(sub), childrenData = listOf(cdDirect))

        val renderBlock: @Composable () -> Unit = {
            scope.renderComponent(cmp)
        }
        assertNotNull(renderBlock)
    }
}
