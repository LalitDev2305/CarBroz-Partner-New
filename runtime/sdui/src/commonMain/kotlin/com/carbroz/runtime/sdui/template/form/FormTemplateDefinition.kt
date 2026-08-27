package com.carbroz.runtime.sdui.template.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.foundation.adaptive.ContentLayoutPolicyResolver
import com.carbroz.foundation.adaptive.LocalAdaptiveEnvironment
import com.carbroz.foundation.designsystem.CarBrozDesignSystem
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
    val contentHorizontalPaddingDp: Float? = null,
    val contentVerticalPaddingDp: Float? = null,
) : CommonNodePropertyOwner

object FormTemplateDefinition : TemplateDefinition<FormTemplateProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("FORM_TEMPLATE")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<FormTemplateProperties> {
        val common = when (val decoded = CommonNodePropertiesDecoder.decode(raw)) {
            is CommonPropertiesDecodeResult.Success -> decoded.value
            is CommonPropertiesDecodeResult.Failure -> return PropertyDecodeResult.Failure(decoded.reason)
        }
        fun positiveOrZero(key: String): Float? {
            val value = (raw[key] as? JsonPrimitive)?.floatOrNull ?: return null
            if (value < 0f) return Float.NaN
            return value
        }
        val maxContentWidth = (raw["maxContentWidth"] as? JsonPrimitive)?.floatOrNull
        if (maxContentWidth != null && maxContentWidth <= 0f) {
            return PropertyDecodeResult.Failure("FORM_TEMPLATE 'maxContentWidth' must be positive")
        }
        val horizontal = positiveOrZero("contentHorizontalPadding")
        val vertical = positiveOrZero("contentVerticalPadding")
        if (horizontal?.isNaN() == true) return PropertyDecodeResult.Failure("FORM_TEMPLATE 'contentHorizontalPadding' must be non-negative")
        if (vertical?.isNaN() == true) return PropertyDecodeResult.Failure("FORM_TEMPLATE 'contentVerticalPadding' must be non-negative")
        return PropertyDecodeResult.Success(FormTemplateProperties(common, maxContentWidth, horizontal, vertical))
    }

    @Composable
    override fun RenderTemplate(node: Template, context: SduiRenderContext, children: @Composable () -> Unit): Boolean {
        val properties = node.properties as? FormTemplateProperties ?: return false
        if (!properties.common.visible) return true

        val environment = LocalAdaptiveEnvironment.current
        val policy = ContentLayoutPolicyResolver.forWidthClass(environment.widthClass)
        val maxWidth = properties.maxContentWidthDp?.dp ?: policy.maxReadableWidth
        val horizontal = properties.contentHorizontalPaddingDp?.dp ?: policy.horizontalMargin
        val vertical = properties.contentVerticalPaddingDp?.dp ?: CarBrozDesignSystem.spacing.large

        Box(
            modifier = Modifier.fillMaxSize().applyCommonNodeProperties(properties.common),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth)
                    .padding(horizontal = horizontal, vertical = vertical),
            ) {
                children()
            }
        }
        return true
    }
}
