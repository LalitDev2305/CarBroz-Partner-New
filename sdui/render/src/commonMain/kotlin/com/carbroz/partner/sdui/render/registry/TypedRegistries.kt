package com.carbroz.partner.sdui.render.registry

import com.carbroz.partner.sdui.render.renderer.child.ChildRenderer
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.renderer.component.ComponentRenderer
import com.carbroz.partner.sdui.render.renderer.subcomponent.SubComponentRenderer
import com.carbroz.partner.sdui.render.renderer.template.TemplateRenderer

public class TemplateRendererRegistry(entries: Map<String, TemplateRenderer>) {
    private val delegate = RendererRegistry(entries)
    public fun resolve(type: String): TemplateRenderer? = delegate.resolve(type)
    public fun contains(type: String): Boolean = delegate.contains(type)
}

public class ComponentRendererRegistry(entries: Map<String, ComponentRenderer>) {
    private val delegate = RendererRegistry(entries)
    public fun resolve(type: String): ComponentRenderer? = delegate.resolve(type)
    public fun contains(type: String): Boolean = delegate.contains(type)
}

public class SubComponentRendererRegistry(entries: Map<String, SubComponentRenderer>) {
    private val delegate = RendererRegistry(entries)
    public fun resolve(type: String): SubComponentRenderer? = delegate.resolve(type)
    public fun contains(type: String): Boolean = delegate.contains(type)
}

public class ChildRendererRegistry(entries: Map<String, ChildRenderer>) {
    private val delegate = RendererRegistry(entries)
    public fun resolve(type: String): ChildRenderer? = delegate.resolve(type)
    public fun contains(type: String): Boolean = delegate.contains(type)
}

public class ChildrenDataRendererRegistry(entries: Map<String, ChildrenDataRenderer>) {
    private val delegate = RendererRegistry(entries)
    public fun resolve(type: String): ChildrenDataRenderer? = delegate.resolve(type)
    public fun contains(type: String): Boolean = delegate.contains(type)
}
