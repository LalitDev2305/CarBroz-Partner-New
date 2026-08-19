package com.carbroz.partner.sdui.engine.validation

import com.carbroz.partner.sdui.engine.raw.RawChildrenDataDto
import com.carbroz.partner.sdui.engine.raw.RawComponentDto
import com.carbroz.partner.sdui.engine.raw.RawScreenDto
import com.carbroz.partner.sdui.engine.raw.RawSubComponentDto
import com.carbroz.partner.sdui.engine.raw.RawTemplateDto
import com.carbroz.partner.sdui.engine.result.SduiParseError
import kotlin.test.Test
import kotlin.test.assertTrue

class CrossLevelNodeIdUniquenessTest {

    private val validator: SduiValidator = DefaultSduiValidator()

    @Test
    fun testDuplicateIdAcrossComponentAndChildrenDataFailsValidation() {
        val rawScreen = RawScreenDto(
            schemaVersion = 1,
            screenId = "screen_1",
            title = "Test",
            showBack = false,
            template = RawTemplateDto(
                templateId = "tpl_1",
                templateType = "form",
                width = "fill",
                height = "fill",
                axis = "vertical",
                components = listOf(
                    RawComponentDto(
                        componentId = "duplicate_id",
                        componentType = "section",
                        childrenData = listOf(
                            RawChildrenDataDto(
                                childrenDataId = "duplicate_id",
                                childrenDataType = "text"
                            )
                        )
                    )
                )
            )
        )

        val error = validator.validate(rawScreen, 100, SduiValidationPolicy())
        assertTrue(error is SduiParseError.DuplicateNodeId)
    }
}
