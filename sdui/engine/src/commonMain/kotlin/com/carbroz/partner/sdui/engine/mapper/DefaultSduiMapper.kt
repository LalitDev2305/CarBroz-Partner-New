package com.carbroz.partner.sdui.engine.mapper

import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.sdui.engine.model.*
import com.carbroz.partner.sdui.engine.raw.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

public class DefaultSduiMapper : SduiMapper {

    override fun map(rawScreen: RawScreenDto): SduiScreen {
        fun parseDimension(dim: String?): DimensionSpec = when {
            dim.isNullOrBlank() -> DimensionSpec.Wrap
            dim.lowercase() == "fill" -> DimensionSpec.Fill
            dim.lowercase() == "wrap" -> DimensionSpec.Wrap
            dim.endsWith("%") -> {
                val fraction = dim.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 1f
                DimensionSpec.Fraction(fraction)
            }
            dim.startsWith("token:") -> DimensionSpec.Token(dim.removePrefix("token:"))
            dim.toIntOrNull() != null -> DimensionSpec.Fixed(dim.toInt().dp)
            else -> DimensionSpec.Wrap
        }

        fun parseSpacing(spacing: Int?): SpacingSpec = when {
            spacing == null || spacing <= 0 -> SpacingSpec.Fixed(0.dp)
            else -> SpacingSpec.Fixed(spacing.dp)
        }

        fun parseEdgeSpacing(edge: RawEdgeSpacingDto?): SduiEdgeSpacing {
            if (edge == null) return SduiEdgeSpacing()
            return SduiEdgeSpacing(
                top = edge.top ?: 0,
                bottom = edge.bottom ?: 0,
                start = edge.start ?: 0,
                end = edge.end ?: 0
            )
        }

        fun parseAxis(axis: String?): LayoutAxis = when (axis?.lowercase()) {
            "horizontal" -> LayoutAxis.HORIZONTAL
            else -> LayoutAxis.VERTICAL
        }

        fun parseAlignment(alignment: String?): LayoutAlignment = when (alignment?.lowercase()?.trim()) {
            "center" -> LayoutAlignment.CENTER
            "end" -> LayoutAlignment.END
            else -> LayoutAlignment.START
        }

        fun parseArrangement(arrangement: String?): LayoutArrangement = when (arrangement?.lowercase()?.trim()) {
            "center" -> LayoutArrangement.CENTER
            "end" -> LayoutArrangement.END
            "space_between" -> LayoutArrangement.SPACE_BETWEEN
            "space_around" -> LayoutArrangement.SPACE_AROUND
            "space_evenly" -> LayoutArrangement.SPACE_EVENLY
            else -> LayoutArrangement.START
        }

        fun parseAction(action: RawActionDto?): SduiAction? {
            if (action == null || action.api.isNullOrBlank()) return null
            return SduiAction(
                api = action.api,
                templateId = action.templateId,
                templateType = action.templateType,
                payload = action.payload
            )
        }

        fun parseParentAction(pa: RawParentActionDto?): SduiParentAction? {
            if (pa == null || pa.targetId.isNullOrBlank()) return null
            return SduiParentAction(targetId = pa.targetId)
        }

        fun hasAcceptsParentAction(props: kotlinx.serialization.json.JsonObject?): Boolean {
            if (props == null) return false
            val elem = props["accepts_parent_action"] ?: return false
            return (elem as? JsonPrimitive)?.booleanOrNull ?: false
        }

        fun mapChildrenData(cd: RawChildrenDataDto): SduiChildrenData {
            val accepts = hasAcceptsParentAction(cd.properties)
            return SduiChildrenData(
                id = cd.childrenDataId!!,
                childrenDataType = cd.childrenDataType!!,
                width = parseDimension(cd.width),
                height = parseDimension(cd.height),
                padding = parseEdgeSpacing(cd.padding),
                margin = parseEdgeSpacing(cd.margin),
                visible = cd.visible ?: true,
                enabled = cd.enabled ?: true,
                properties = cd.properties,
                action = parseAction(cd.action),
                parentAction = parseParentAction(cd.parentAction),
                acceptsParentAction = accepts
            )
        }

        fun mapChild(ch: RawChildDto): SduiChild {
            val accepts = hasAcceptsParentAction(ch.properties)
            val childrenDataList = ch.childrenData?.map { mapChildrenData(it) } ?: emptyList()
            return SduiChild(
                id = ch.childId!!,
                childType = ch.childType!!,
                width = parseDimension(ch.width),
                height = parseDimension(ch.height),
                axis = parseAxis(ch.axis),
                alignment = parseAlignment(ch.alignment),
                arrangement = parseArrangement(ch.arrangement),
                padding = parseEdgeSpacing(ch.padding),
                margin = parseEdgeSpacing(ch.margin),
                gap = parseSpacing(ch.gap),
                visible = ch.visible ?: true,
                enabled = ch.enabled ?: true,
                properties = ch.properties,
                action = parseAction(ch.action),
                parentAction = parseParentAction(ch.parentAction),
                childrenData = childrenDataList,
                acceptsParentAction = accepts
            )
        }

        fun mapSubComponent(s: RawSubComponentDto): SduiSubComponent {
            val accepts = hasAcceptsParentAction(s.properties)
            val childrenList = s.children?.map { mapChild(it) } ?: emptyList()
            val childrenDataList = s.childrenData?.map { mapChildrenData(it) } ?: emptyList()
            return SduiSubComponent(
                id = s.subcomponentId!!,
                subcomponentType = s.subcomponentType!!,
                width = parseDimension(s.width),
                height = parseDimension(s.height),
                axis = parseAxis(s.axis),
                alignment = parseAlignment(s.alignment),
                arrangement = parseArrangement(s.arrangement),
                padding = parseEdgeSpacing(s.padding),
                margin = parseEdgeSpacing(s.margin),
                gap = parseSpacing(s.gap),
                visible = s.visible ?: true,
                enabled = s.enabled ?: true,
                properties = s.properties,
                action = parseAction(s.action),
                parentAction = parseParentAction(s.parentAction),
                children = childrenList,
                childrenData = childrenDataList,
                acceptsParentAction = accepts
            )
        }

        fun mapComponent(c: RawComponentDto): SduiComponent {
            val accepts = hasAcceptsParentAction(c.properties)
            val subcomponentsList = c.subcomponents?.map { mapSubComponent(it) } ?: emptyList()
            val childrenDataList = c.childrenData?.map { mapChildrenData(it) } ?: emptyList()
            return SduiComponent(
                id = c.componentId!!,
                componentType = c.componentType!!,
                width = parseDimension(c.width),
                height = parseDimension(c.height),
                axis = parseAxis(c.axis),
                alignment = parseAlignment(c.alignment),
                arrangement = parseArrangement(c.arrangement),
                padding = parseEdgeSpacing(c.padding),
                margin = parseEdgeSpacing(c.margin),
                gap = parseSpacing(c.gap),
                visible = c.visible ?: true,
                enabled = c.enabled ?: true,
                properties = c.properties,
                action = parseAction(c.action),
                parentAction = parseParentAction(c.parentAction),
                subcomponents = subcomponentsList,
                childrenData = childrenDataList,
                acceptsParentAction = accepts
            )
        }

        fun mapTemplate(t: RawTemplateDto): SduiTemplate {
            val accepts = hasAcceptsParentAction(t.properties)
            val componentsList = t.components?.map { mapComponent(it) } ?: emptyList()
            return SduiTemplate(
                id = t.templateId!!,
                templateType = t.templateType!!,
                width = parseDimension(t.width),
                height = parseDimension(t.height),
                axis = parseAxis(t.axis),
                alignment = parseAlignment(t.alignment),
                arrangement = parseArrangement(t.arrangement),
                padding = parseEdgeSpacing(t.padding),
                margin = parseEdgeSpacing(t.margin),
                gap = parseSpacing(t.gap),
                properties = t.properties,
                components = componentsList,
                acceptsParentAction = accepts
            )
        }

        val rawT = rawScreen.template!!
        val mappedTemplate = mapTemplate(rawT)

        val mappedTheme = rawScreen.theme?.let { th ->
            if (th.themeId.isNullOrBlank()) null
            else SduiTheme(
                themeId = th.themeId,
                backgroundColor = th.backgroundColor ?: "#FFFFFF",
                primaryColor = th.primaryColor ?: "#000000",
                gradient = th.gradient?.let { g ->
                    if (!g.colors.isNullOrEmpty()) SduiGradient(colors = g.colors, angle = g.angle ?: 0f)
                    else null
                }
            )
        }

        val mappedBack = rawScreen.back?.let { b ->
            if (b.api.isNullOrBlank()) null
            else SduiBackPolicy(
                api = b.api,
                templateId = b.templateId ?: "",
                templateType = b.templateType ?: ""
            )
        }

        val mappedRefresh = rawScreen.refresh?.let { r ->
            if (r.api.isNullOrBlank()) null
            else SduiRefreshPolicy(api = r.api)
        }

        return SduiScreen(
            schemaVersion = rawScreen.schemaVersion ?: 1,
            screenId = rawScreen.screenId!!,
            title = rawScreen.title ?: "",
            showBack = rawScreen.showBack ?: false,
            back = mappedBack,
            refresh = mappedRefresh,
            theme = mappedTheme,
            template = mappedTemplate
        )
    }
}

