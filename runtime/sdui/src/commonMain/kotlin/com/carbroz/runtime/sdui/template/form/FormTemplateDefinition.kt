package com.carbroz.runtime.sdui.template.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carbroz.foundation.adaptive.ContentLayoutPolicyResolver
import com.carbroz.foundation.adaptive.LocalAdaptiveEnvironment
import com.carbroz.foundation.designsystem.CarBrozDesignSystem
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.TemplateDefinition
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.Template
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject

/** Semantic properties owned only by FORM_TEMPLATE. */
data object FormTemplateProperties : NodeProperties

object FormTemplateDefinition : TemplateDefinition<FormTemplateProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("FORM_TEMPLATE")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<FormTemplateProperties> =
        PropertyDecodeResult.Success(FormTemplateProperties)

    @Composable
    override fun RenderTemplate(
        node: Template,
        context: SduiRenderContext,
        children: @Composable () -> Unit,
    ): Boolean {
        if (node.properties !== FormTemplateProperties) return false

        val environment = LocalAdaptiveEnvironment.current
        val policy = ContentLayoutPolicyResolver.forWidthClass(environment.widthClass)
        val spacing = CarBrozDesignSystem.spacing

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = policy.maxReadableWidth)
                    .padding(
                        horizontal = policy.horizontalMargin,
                        vertical = spacing.large,
                    ),
            ) {
                children()
            }
        }
        return true
    }
}
