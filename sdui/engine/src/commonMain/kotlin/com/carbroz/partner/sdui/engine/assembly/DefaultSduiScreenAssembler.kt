package com.carbroz.partner.sdui.engine.assembly

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.sdui.engine.mapper.DefaultSduiMapper
import com.carbroz.partner.sdui.engine.mapper.SduiMapper
import com.carbroz.partner.sdui.engine.parser.DefaultSduiParser
import com.carbroz.partner.sdui.engine.parser.SduiParser
import com.carbroz.partner.sdui.engine.result.SduiParseError
import com.carbroz.partner.sdui.engine.result.SduiParseResult
import com.carbroz.partner.sdui.engine.validation.DefaultSduiValidator
import com.carbroz.partner.sdui.engine.validation.SduiValidationPolicy
import com.carbroz.partner.sdui.engine.validation.SduiValidator

public class DefaultSduiScreenAssembler(
    private val parser: SduiParser = DefaultSduiParser(),
    private val validator: SduiValidator = DefaultSduiValidator(),
    private val mapper: SduiMapper = DefaultSduiMapper(),
    private val logger: BoundLogger? = null
) : SduiScreenAssembler {

    override fun assemble(
        jsonString: String,
        policy: SduiValidationPolicy
    ): SduiParseResult {
        val jsonLength = jsonString.encodeToByteArray().size
        val rawDto = try {
            parser.parse(jsonString)
        } catch (e: Exception) {
            logger?.log(
                level = LogLevel.WARN,
                sourceFunction = "assemble",
                category = LogCategory.EXECUTION,
                event = "SDUI_PARSE_FAILED",
                message = "SDUI JSON parse failed: ${e.message}"
            )
            return SduiParseResult.Failure(
                SduiParseError.MalformedJson("Failed to deserialize SDUI JSON: ${e.message}")
            )
        }

        val valError = validator.validate(rawDto, jsonLength, policy)
        if (valError != null) {
            logger?.log(
                level = LogLevel.WARN,
                sourceFunction = "assemble",
                category = LogCategory.EXECUTION,
                event = "SDUI_VALIDATION_FAILED",
                message = "SDUI validation failed [${valError.code}]: ${valError.message}"
            )
            return SduiParseResult.Failure(valError)
        }

        val screenModel = try {
            mapper.map(rawDto)
        } catch (e: Exception) {
            logger?.error(
                sourceFunction = "assemble",
                category = LogCategory.EXECUTION,
                event = "SDUI_MAPPING_FAILED",
                message = "SDUI mapping failed: ${e.message}",
                throwable = e
            )
            return SduiParseResult.Failure(
                SduiParseError.InvalidPropertyValue("Mapping error: ${e.message}")
            )
        }

        val nodeIndexMap = mutableMapOf<String, com.carbroz.partner.sdui.engine.model.SduiNodeRef>()

        fun indexChildrenData(cdList: List<com.carbroz.partner.sdui.engine.model.SduiChildrenData>) {
            for (cd in cdList) {
                nodeIndexMap[cd.id] = cd
            }
        }

        fun indexChild(childList: List<com.carbroz.partner.sdui.engine.model.SduiChild>) {
            for (child in childList) {
                nodeIndexMap[child.id] = child
                indexChildrenData(child.childrenData)
            }
        }

        fun indexSubComponent(subList: List<com.carbroz.partner.sdui.engine.model.SduiSubComponent>) {
            for (sub in subList) {
                nodeIndexMap[sub.id] = sub
                indexChild(sub.children)
                indexChildrenData(sub.childrenData)
            }
        }

        for (comp in screenModel.template.components) {
            nodeIndexMap[comp.id] = comp
            indexSubComponent(comp.subcomponents)
            indexChildrenData(comp.childrenData)
        }

        val assembledScreen = AssembledSduiScreen(
            screen = screenModel,
            nodeIndex = nodeIndexMap
        )

        logger?.info(
            sourceFunction = "assemble",
            category = LogCategory.EXECUTION,
            event = "SDUI_ASSEMBLED_SUCCESS",
            message = "SDUI screen '${screenModel.screenId}' assembled successfully with ${assembledScreen.nodeIndex.size} nodes"
        )

        return SduiParseResult.Success(assembledScreen)
    }
}
