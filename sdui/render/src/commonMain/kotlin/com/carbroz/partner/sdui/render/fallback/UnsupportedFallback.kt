package com.carbroz.partner.sdui.render.fallback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.scope.RenderScope

public object UnsupportedFallback {
    private const val IS_DEBUG: Boolean = true

    @Composable
    public fun renderUnsupportedTemplate(template: SduiTemplate, scope: RenderScope) {
        if (IS_DEBUG) {
            DebugLabel("Unsupported Template: '${template.templateType}'")
        }
        Column {
            for (comp in template.components) {
                scope.renderComponent(comp)
            }
        }
    }

    @Composable
    public fun renderUnsupportedComponent(component: SduiComponent, scope: RenderScope) {
        if (IS_DEBUG) {
            DebugLabel("Unsupported Component: '${component.componentType}'")
        }
        Column {
            for (sub in component.subcomponents) {
                scope.renderSubComponent(sub)
            }
            for (cd in component.childrenData) {
                scope.renderChildrenData(cd)
            }
        }
    }

    @Composable
    public fun renderUnsupportedSubComponent(subComponent: SduiSubComponent, scope: RenderScope) {
        if (IS_DEBUG) {
            DebugLabel("Unsupported SubComponent: '${subComponent.subcomponentType}'")
        }
        Column {
            for (ch in subComponent.children) {
                scope.renderChild(ch)
            }
            for (cd in subComponent.childrenData) {
                scope.renderChildrenData(cd)
            }
        }
    }

    @Composable
    public fun renderUnsupportedChild(child: SduiChild, scope: RenderScope) {
        if (IS_DEBUG) {
            DebugLabel("Unsupported Child: '${child.childType}'")
        }
        Row {
            for (cd in child.childrenData) {
                scope.renderChildrenData(cd)
            }
        }
    }

    @Composable
    public fun renderUnsupportedChildrenData(type: String) {
        if (IS_DEBUG) {
            DebugLabel("Unsupported Primitive: '$type'")
        }
    }

    @Composable
    private fun DebugLabel(text: String) {
        Box(
            modifier = Modifier.background(Color(0xFFFFEBEE))
        ) {
            Text(
                text = "[$text]",
                color = Color(0xFFC62828),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
