package com.carbroz.partner.sdui.host.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carbroz.partner.sdui.host.controller.SduiScreenHostController
import com.carbroz.partner.sdui.host.model.SduiHostState
import com.carbroz.partner.sdui.render.SduiRenderers
import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.renderer.screen.SduiScreenRenderer
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

@Composable
public fun SduiScreenHost(
    controller: SduiScreenHostController,
    templateRegistry: TemplateRendererRegistry = SduiRenderers.defaultTemplates,
    componentRegistry: ComponentRendererRegistry = SduiRenderers.defaultComponents,
    subComponentRegistry: SubComponentRendererRegistry = SduiRenderers.defaultSubComponents,
    childRegistry: ChildRendererRegistry = SduiRenderers.defaultChildren,
    childrenDataRegistry: ChildrenDataRendererRegistry = SduiRenderers.defaultChildrenData,
    modifier: Modifier = Modifier
) {
    val state by controller.hostState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = state) {
            is SduiHostState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SduiHostState.Content -> {
                val runtime = controller.runtime
                val runtimeState = runtime?.state?.collectAsState()?.value
                val snapshot = runtimeState?.snapshot ?: SduiRenderSnapshot()

                SduiScreenRenderer(
                    assembledScreen = currentState.assembledScreen,
                    templateRegistry = templateRegistry,
                    componentRegistry = componentRegistry,
                    subComponentRegistry = subComponentRegistry,
                    childRegistry = childRegistry,
                    childrenDataRegistry = childrenDataRegistry,
                    snapshot = snapshot,
                    eventSink = { event -> controller.onUiEvent(event) },
                    modifier = Modifier.fillMaxSize()
                )
                if (currentState.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter))
                }
            }
            is SduiHostState.Error -> {
                Text(
                    text = currentState.message,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
