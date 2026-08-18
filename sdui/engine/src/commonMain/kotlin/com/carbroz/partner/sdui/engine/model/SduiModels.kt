package com.carbroz.partner.sdui.engine.model

import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import kotlinx.serialization.json.JsonObject

public data class SduiScreen(
    val schemaVersion: Int,
    val screenId: String,
    val title: String,
    val showBack: Boolean,
    val back: SduiBackPolicy?,
    val refresh: SduiRefreshPolicy?,
    val theme: SduiTheme?,
    val template: SduiTemplate
)

public data class SduiBackPolicy(
    val api: String,
    val templateId: String,
    val templateType: String
)

public data class SduiRefreshPolicy(
    val api: String
)

public data class SduiTheme(
    val themeId: String,
    val backgroundColor: String,
    val primaryColor: String,
    val gradient: SduiGradient?
)

public data class SduiGradient(
    val colors: List<String>,
    val angle: Float
)

public sealed interface SduiNodeRef {
    val id: String
    val acceptsParentAction: Boolean
}

public data class SduiTemplate(
    override val id: String,
    val templateType: String,
    val width: DimensionSpec,
    val height: DimensionSpec,
    val axis: LayoutAxis,
    val padding: SduiEdgeSpacing,
    val margin: SduiEdgeSpacing,
    val gap: SpacingSpec,
    val properties: JsonObject?,
    val components: List<SduiComponent>,
    override val acceptsParentAction: Boolean = false
) : SduiNodeRef

public data class SduiComponent(
    override val id: String,
    val componentType: String,
    val width: DimensionSpec,
    val height: DimensionSpec,
    val axis: LayoutAxis,
    val padding: SduiEdgeSpacing,
    val margin: SduiEdgeSpacing,
    val gap: SpacingSpec,
    val visible: Boolean,
    val enabled: Boolean,
    val properties: JsonObject?,
    val action: SduiAction?,
    val parentAction: SduiParentAction?,
    val subcomponents: List<SduiSubComponent>,
    val childrenData: List<SduiChildrenData>,
    override val acceptsParentAction: Boolean
) : SduiNodeRef

public data class SduiSubComponent(
    override val id: String,
    val subcomponentType: String,
    val width: DimensionSpec,
    val height: DimensionSpec,
    val axis: LayoutAxis,
    val padding: SduiEdgeSpacing,
    val margin: SduiEdgeSpacing,
    val gap: SpacingSpec,
    val visible: Boolean,
    val enabled: Boolean,
    val properties: JsonObject?,
    val action: SduiAction?,
    val parentAction: SduiParentAction?,
    val children: List<SduiChild>,
    val childrenData: List<SduiChildrenData>,
    override val acceptsParentAction: Boolean
) : SduiNodeRef

public data class SduiChild(
    override val id: String,
    val childType: String,
    val width: DimensionSpec,
    val height: DimensionSpec,
    val axis: LayoutAxis,
    val padding: SduiEdgeSpacing,
    val margin: SduiEdgeSpacing,
    val gap: SpacingSpec,
    val visible: Boolean,
    val enabled: Boolean,
    val properties: JsonObject?,
    val action: SduiAction?,
    val parentAction: SduiParentAction?,
    val childrenData: List<SduiChildrenData>,
    override val acceptsParentAction: Boolean
) : SduiNodeRef

public data class SduiChildrenData(
    override val id: String,
    val childrenDataType: String,
    val width: DimensionSpec,
    val height: DimensionSpec,
    val padding: SduiEdgeSpacing,
    val margin: SduiEdgeSpacing,
    val visible: Boolean,
    val enabled: Boolean,
    val properties: JsonObject?,
    val action: SduiAction?,
    val parentAction: SduiParentAction?,
    override val acceptsParentAction: Boolean
) : SduiNodeRef

public data class SduiEdgeSpacing(
    val top: Int = 0,
    val bottom: Int = 0,
    val start: Int = 0,
    val end: Int = 0
)

public enum class LayoutAxis {
    VERTICAL,
    HORIZONTAL
}

public data class SduiAction(
    val api: String,
    val templateId: String?,
    val templateType: String?,
    val payload: JsonObject?
)

public data class SduiParentAction(
    val targetId: String
)

public sealed interface SduiBindable<out T> {
    public data class Static<out T>(val value: T) : SduiBindable<T>
    public data class Dynamic(val expression: String) : SduiBindable<Nothing>
}
