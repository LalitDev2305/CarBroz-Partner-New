package com.carbroz.partner.sdui.render.fakes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.renderer.child.ChildRenderer
import com.carbroz.partner.sdui.render.renderer.component.ComponentRenderer
import com.carbroz.partner.sdui.render.renderer.subcomponent.SubComponentRenderer
import com.carbroz.partner.sdui.render.renderer.template.TemplateRenderer
import com.carbroz.partner.sdui.render.scope.RenderScope

public object FakeTemplateRenderer : TemplateRenderer {
    @Composable
    override fun render(template: SduiTemplate, scope: RenderScope) {
        Column {
            for (comp in template.components) {
                scope.renderComponent(comp)
            }
        }
    }
}

public object FakeComponentRenderer : ComponentRenderer {
    @Composable
    override fun render(component: SduiComponent, scope: RenderScope) {
        Column {
            for (sub in component.subcomponents) {
                scope.renderSubComponent(sub)
            }
            for (cd in component.childrenData) {
                scope.renderChildrenData(cd)
            }
        }
    }
}

public object FakeSubComponentRenderer : SubComponentRenderer {
    @Composable
    override fun render(subComponent: SduiSubComponent, scope: RenderScope) {
        Column {
            for (ch in subComponent.children) {
                scope.renderChild(ch)
            }
            for (cd in subComponent.childrenData) {
                scope.renderChildrenData(cd)
            }
        }
    }
}

public object FakeChildRenderer : ChildRenderer {
    @Composable
    override fun render(child: SduiChild, scope: RenderScope) {
        Row {
            for (cd in child.childrenData) {
                scope.renderChildrenData(cd)
            }
        }
    }
}
