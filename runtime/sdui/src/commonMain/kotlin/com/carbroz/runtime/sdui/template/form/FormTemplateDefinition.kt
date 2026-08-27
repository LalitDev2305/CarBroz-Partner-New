package com.carbroz.runtime.sdui.template.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.foundation.adaptive.ContentLayoutPolicyResolver
import com.carbroz.foundation.adaptive.LocalAdaptiveEnvironment
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.TemplateDefinition
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.Template
import com.carbroz.runtime.sdui.properties.CommonNodeProperties
import com.carbroz.runtime.sdui.properties.CommonNodePropertiesDecoder
import com.carbroz.runtime.sdui.properties.CommonNodePropertyOwner
import com.carbroz.runtime.sdui.properties.CommonPropertiesDecodeResult
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import com.carbroz.runtime.sdui.rendering.applyCommonNodeProperties
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

data class FormTemplateProperties(
    override val common: CommonNodeProperties,
    val maxContentWidthDp: Float? = null,
) : CommonNodePropertyOwner

object FormTemplateDefinition : TemplateDefinition<FormTemplateProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("FORM_TEMPLATE")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<FormTemplateProperties> {
        val common = when (val decoded = CommonNodePropertiesDecoder.decode(raw)) {
            is CommonPropertiesDecodeResult.Success -> decoded.value
            is CommonPropertiesDecodeResult.Failure -> return PropertyDecodeResult.Failure(decoded.reason)
        }
        val maxContentWidth = (raw["maxContentWidth"] as? JsonPrimitive)?.floatOrNull
        if (maxContentWidth != null && maxContentWidth <= 0f) {
            return PropertyDecodeResult.Failure("FORM_TEMPLATE 'maxContentWidth' must be positive")
        }
        return PropertyDecodeResult.Success(FormTemplateProperties(common, maxContentWidth))
    }

    @Composable
    override fun RenderTemplate(node: Template, context: SduiRenderContext, children: @Composable () -> Unit): Boolean {
        val properties = node.properties as? FormTemplateProperties ?: return false
        if (!properties.common.visible) return true

        val environment = LocalAdaptiveEnvironment.current
        val policy = ContentLayoutPolicyResolver.forWidthClass(environment.widthClass)
        val maxWidth = properties.maxContentWidthDp?.dp ?: policy.maxReadableWidth

        Box(
            modifier = Modifier.fillMaxSize().applyCommonNodeProperties(properties.common),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().widthIn(max = maxWidth),
            ) {
                children()
            }
        }
        return true
    }
}
