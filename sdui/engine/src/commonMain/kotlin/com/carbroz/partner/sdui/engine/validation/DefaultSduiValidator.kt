package com.carbroz.partner.sdui.engine.validation

import com.carbroz.partner.sdui.engine.raw.*
import com.carbroz.partner.sdui.engine.result.SduiParseError
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

public class DefaultSduiValidator : SduiValidator {

    override fun validate(
        rawScreen: RawScreenDto,
        jsonLengthBytes: Int,
        policy: SduiValidationPolicy
    ): SduiParseError? {
        if (jsonLengthBytes > policy.maxPayloadSizeBytes) {
            return SduiParseError.PayloadLimitExceeded(
                "Payload size $jsonLengthBytes bytes exceeds maximum policy limit of ${policy.maxPayloadSizeBytes} bytes"
            )
        }

        val version = rawScreen.schemaVersion
            ?: return SduiParseError.MissingRequiredField("schema_version is required")

        if (version > policy.supportedSchemaVersion) {
            return SduiParseError.UnsupportedSchemaVersion(
                "Schema version $version exceeds supported maximum version ${policy.supportedSchemaVersion}"
            )
        }

        val screenId = rawScreen.screenId
        if (screenId.isNullOrBlank()) {
            return SduiParseError.MissingRequiredField("screen_id is required and cannot be blank")
        }

        val template = rawScreen.template
            ?: return SduiParseError.MissingRequiredField("template is required")

        val collectedNodeIds = mutableMapOf<String, Boolean>()
        var nodeCount = 0

        fun collectChildrenDataIds(cd: RawChildrenDataDto): SduiParseError? {
            val cdId = cd.childrenDataId
            if (cdId.isNullOrBlank()) return SduiParseError.MissingRequiredField("children_data_id is required")
            val cdType = cd.childrenDataType
            if (cdType.isNullOrBlank()) return SduiParseError.MissingRequiredField("children_data_type is required")

            if (collectedNodeIds.containsKey(cdId)) {
                return SduiParseError.DuplicateNodeId("Duplicate node identity found: '$cdId'")
            }
            val accepts = cd.properties?.get("accepts_parent_action")?.let {
                (it as? JsonPrimitive)?.booleanOrNull ?: false
            } ?: false
            collectedNodeIds[cdId] = accepts
            nodeCount++
            return null
        }

        fun collectChildIds(ch: RawChildDto): SduiParseError? {
            val chId = ch.childId
            if (chId.isNullOrBlank()) return SduiParseError.MissingRequiredField("child_id is required")
            val chType = ch.childType
            if (chType.isNullOrBlank()) return SduiParseError.MissingRequiredField("child_type is required")

            if (collectedNodeIds.containsKey(chId)) {
                return SduiParseError.DuplicateNodeId("Duplicate node identity found: '$chId'")
            }
            val accepts = ch.properties?.get("accepts_parent_action")?.let {
                (it as? JsonPrimitive)?.booleanOrNull ?: false
            } ?: false
            collectedNodeIds[chId] = accepts
            nodeCount++

            ch.childrenData?.forEach { cd ->
                val err = collectChildrenDataIds(cd)
                if (err != null) return err
            }
            return null
        }

        fun collectSubComponentIds(s: RawSubComponentDto): SduiParseError? {
            val sId = s.subcomponentId
            if (sId.isNullOrBlank()) return SduiParseError.MissingRequiredField("subcomponent_id is required")
            val sType = s.subcomponentType
            if (sType.isNullOrBlank()) return SduiParseError.MissingRequiredField("subcomponent_type is required")

            if (collectedNodeIds.containsKey(sId)) {
                return SduiParseError.DuplicateNodeId("Duplicate node identity found: '$sId'")
            }
            val accepts = s.properties?.get("accepts_parent_action")?.let {
                (it as? JsonPrimitive)?.booleanOrNull ?: false
            } ?: false
            collectedNodeIds[sId] = accepts
            nodeCount++

            if (!s.children.isNullOrEmpty() && !s.childrenData.isNullOrEmpty()) {
                return SduiParseError.InvalidHierarchy("SubComponent '$sId' cannot specify both children and children_data")
            }

            s.children?.forEach { child ->
                val err = collectChildIds(child)
                if (err != null) return err
            }
            s.childrenData?.forEach { cd ->
                val err = collectChildrenDataIds(cd)
                if (err != null) return err
            }
            return null
        }

        fun collectComponentIds(c: RawComponentDto): SduiParseError? {
            val cId = c.componentId
            if (cId.isNullOrBlank()) return SduiParseError.MissingRequiredField("component_id is required")
            val cType = c.componentType
            if (cType.isNullOrBlank()) return SduiParseError.MissingRequiredField("component_type is required")

            if (collectedNodeIds.containsKey(cId)) {
                return SduiParseError.DuplicateNodeId("Duplicate node identity found: '$cId'")
            }
            val accepts = c.properties?.get("accepts_parent_action")?.let {
                (it as? JsonPrimitive)?.booleanOrNull ?: false
            } ?: false
            collectedNodeIds[cId] = accepts
            nodeCount++

            if (!c.subcomponents.isNullOrEmpty() && !c.childrenData.isNullOrEmpty()) {
                return SduiParseError.InvalidHierarchy("Component '$cId' cannot specify both subcomponents and children_data")
            }

            c.subcomponents?.forEach { sub ->
                val err = collectSubComponentIds(sub)
                if (err != null) return err
            }
            c.childrenData?.forEach { cd ->
                val err = collectChildrenDataIds(cd)
                if (err != null) return err
            }
            return null
        }

        fun collectTemplateIds(t: RawTemplateDto): SduiParseError? {
            val tId = t.templateId
            if (tId.isNullOrBlank()) return SduiParseError.MissingRequiredField("template_id is required")
            val tType = t.templateType
            if (tType.isNullOrBlank()) return SduiParseError.MissingRequiredField("template_type is required")

            if (collectedNodeIds.containsKey(tId)) {
                return SduiParseError.DuplicateNodeId("Duplicate node identity found: '$tId'")
            }
            val accepts = t.properties?.get("accepts_parent_action")?.let {
                (it as? JsonPrimitive)?.booleanOrNull ?: false
            } ?: false
            collectedNodeIds[tId] = accepts
            nodeCount++

            val components = t.components
            if (components.isNullOrEmpty()) {
                return SduiParseError.MissingRequiredField("template components cannot be empty")
            }

            for (comp in components) {
                val err = collectComponentIds(comp)
                if (err != null) return err
            }
            return null
        }

        val collectErr = collectTemplateIds(template)
        if (collectErr != null) return collectErr

        if (nodeCount > policy.maxNodesPerScreen) {
            return SduiParseError.PayloadLimitExceeded("Node count $nodeCount exceeds policy limit ${policy.maxNodesPerScreen}")
        }

        fun validateAction(action: RawActionDto?): SduiParseError? {
            if (action == null) return null
            val api = action.api
            if (api.isNullOrBlank()) return SduiParseError.MissingRequiredField("action.api is required")
            val tId = action.templateId
            val tType = action.templateType
            if ((tId != null && tType == null) || (tId == null && tType != null)) {
                return SduiParseError.IncompleteTemplateTransition(
                    "action template_id and template_type must be specified together"
                )
            }
            return null
        }

        fun validateParentAction(pa: RawParentActionDto?): SduiParseError? {
            if (pa == null) return null
            val targetId = pa.targetId
            if (targetId.isNullOrBlank()) {
                return SduiParseError.MissingRequiredField("parent_action.target_id is required")
            }
            if (!collectedNodeIds.containsKey(targetId)) {
                return SduiParseError.InvalidParentActionTarget(
                    "parent_action target_id '$targetId' does not exist in screen graph"
                )
            }
            val accepts = collectedNodeIds[targetId] ?: false
            if (!accepts) {
                return SduiParseError.TargetDoesNotAcceptParentAction(
                    "parent_action target '$targetId' does not declare accepts_parent_action: true"
                )
            }
            return null
        }

        fun validateChildrenData(cd: RawChildrenDataDto): SduiParseError? {
            val actErr = validateAction(cd.action)
            if (actErr != null) return actErr
            val paErr = validateParentAction(cd.parentAction)
            if (paErr != null) return paErr
            return null
        }

        fun validateChild(ch: RawChildDto): SduiParseError? {
            val actErr = validateAction(ch.action)
            if (actErr != null) return actErr
            val paErr = validateParentAction(ch.parentAction)
            if (paErr != null) return paErr

            ch.childrenData?.forEach { cd ->
                val err = validateChildrenData(cd)
                if (err != null) return err
            }
            return null
        }

        fun validateSubComponent(s: RawSubComponentDto): SduiParseError? {
            val actErr = validateAction(s.action)
            if (actErr != null) return actErr
            val paErr = validateParentAction(s.parentAction)
            if (paErr != null) return paErr

            s.children?.forEach { child ->
                val err = validateChild(child)
                if (err != null) return err
            }
            s.childrenData?.forEach { cd ->
                val err = validateChildrenData(cd)
                if (err != null) return err
            }
            return null
        }

        fun validateComponent(c: RawComponentDto): SduiParseError? {
            val actErr = validateAction(c.action)
            if (actErr != null) return actErr
            val paErr = validateParentAction(c.parentAction)
            if (paErr != null) return paErr

            c.subcomponents?.forEach { sub ->
                val err = validateSubComponent(sub)
                if (err != null) return err
            }
            c.childrenData?.forEach { cd ->
                val err = validateChildrenData(cd)
                if (err != null) return err
            }
            return null
        }

        for (comp in template.components ?: emptyList()) {
            val err = validateComponent(comp)
            if (err != null) return err
        }

        rawScreen.back?.let { back ->
            val api = back.api
            val tId = back.templateId
            val tType = back.templateType
            if (api.isNullOrBlank() && (tId != null || tType != null)) {
                if ((tId != null && tType == null) || (tId == null && tType != null)) {
                    return SduiParseError.IncompleteTemplateTransition(
                        "back template_id and template_type must be specified together"
                    )
                }
            }
        }

        return null
    }
}
