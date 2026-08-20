package com.carbroz.partner.sdui.render.renderer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.carbroz.partner.core.ui.color.ColorParser
import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen
import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.scope.DefaultRenderScope
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

@Composable
public fun SduiScreenRenderer(
    assembledScreen: AssembledSduiScreen,
    templateRegistry: TemplateRendererRegistry,
    componentRegistry: ComponentRendererRegistry,
    subComponentRegistry: SubComponentRendererRegistry,
    childRegistry: ChildRendererRegistry,
    childrenDataRegistry: ChildrenDataRendererRegistry,
    snapshot: SduiRenderSnapshot,
    eventSink: SduiUiEventSink,
    modifier: Modifier = Modifier
) {
    val scope = DefaultRenderScope(
        templateRegistry = templateRegistry,
        componentRegistry = componentRegistry,
        subComponentRegistry = subComponentRegistry,
        childRegistry = childRegistry,
        childrenDataRegistry = childrenDataRegistry,
        snapshot = snapshot,
        eventSink = eventSink
    )

    val bgColor = ColorParser.parseHexColor(
        colorHex = assembledScreen.screen.theme?.backgroundColor,
        fallback = Color.Unspecified
    )
    val bgModifier = if (bgColor != Color.Unspecified) modifier.background(bgColor) else modifier

    Box(modifier = bgModifier.fillMaxSize()) {
        scope.renderTemplate(assembledScreen.screen.template)
    }
}
