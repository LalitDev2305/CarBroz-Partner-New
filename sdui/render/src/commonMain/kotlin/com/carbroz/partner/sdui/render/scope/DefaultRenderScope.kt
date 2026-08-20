package com.carbroz.partner.sdui.render.scope

import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public class DefaultRenderScope(
    override val resolutionContext: ResolutionContext,
    private val templateRegistry: TemplateRendererRegistry,
    private val componentRegistry: ComponentRendererRegistry,
    private val subComponentRegistry: SubComponentRendererRegistry,
    private val childRegistry: ChildRendererRegistry,
    private val childrenDataRegistry: ChildrenDataRendererRegistry,
    override val snapshot: SduiRenderSnapshot,
    override val eventSink: SduiUiEventSink
) : RenderScope {

    override fun withResolutionContext(resolutionContext: ResolutionContext): RenderScope = DefaultRenderScope(
        resolutionContext = resolutionContext,
        templateRegistry = templateRegistry,
        componentRegistry = componentRegistry,
        subComponentRegistry = subComponentRegistry,
        childRegistry = childRegistry,
        childrenDataRegistry = childrenDataRegistry,
        snapshot = snapshot,
        eventSink = eventSink
    )

    @Composable
    override fun renderTemplate(template: SduiTemplate) {
        val renderer = templateRegistry.resolve(template.templateType)
        if (renderer != null) {
            renderer.render(template, this)
        } else {
            UnsupportedFallback.renderUnsupportedTemplate(template, this)
        }
    }

    @Composable
    override fun renderComponent(component: SduiComponent) {
        if (!snapshot.isNodeVisible(component.id, component.visible)) return
        val renderer = componentRegistry.resolve(component.componentType)
        if (renderer != null) {
            renderer.render(component, this)
        } else {
            UnsupportedFallback.renderUnsupportedComponent(component, this)
        }
    }

    @Composable
    override fun renderSubComponent(subComponent: SduiSubComponent) {
        if (!snapshot.isNodeVisible(subComponent.id, subComponent.visible)) return
        val renderer = subComponentRegistry.resolve(subComponent.subcomponentType)
        if (renderer != null) {
            renderer.render(subComponent, this)
        } else {
            UnsupportedFallback.renderUnsupportedSubComponent(subComponent, this)
        }
    }

    @Composable
    override fun renderChild(child: SduiChild) {
        if (!snapshot.isNodeVisible(child.id, child.visible)) return
        val renderer = childRegistry.resolve(child.childType)
        if (renderer != null) {
            renderer.render(child, this)
        } else {
            UnsupportedFallback.renderUnsupportedChild(child, this)
        }
    }

    @Composable
    override fun renderChildrenData(childrenData: SduiChildrenData) {
        if (!snapshot.isNodeVisible(childrenData.id, childrenData.visible)) return
        val renderer = childrenDataRegistry.resolve(childrenData.childrenDataType)
        if (renderer != null) {
            renderer.render(childrenData, this)
        } else {
            UnsupportedFallback.renderUnsupportedChildrenData(childrenData.childrenDataType)
        }
    }
}
