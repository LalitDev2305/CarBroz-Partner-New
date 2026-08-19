package com.carbroz.partner.sdui.runtime.validation

import com.carbroz.partner.sdui.engine.model.SduiValidationRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SduiValidationResolverTest {

    @Test
    fun testRequiredValidation() {
        val rules = SduiValidationRules(required = true, errorMessage = "Required")
        assertEquals("Required", SduiValidationResolver.validate("", rules))
        assertNull(SduiValidationResolver.validate("9876543210", rules))
    }

    @Test
    fun testMinLengthValidation() {
        val rules = SduiValidationRules(minLength = 5)
        assertEquals("Minimum length is 5 characters", SduiValidationResolver.validate("123", rules))
        assertNull(SduiValidationResolver.validate("12345", rules))
    }

    @Test
    fun testMaxLengthValidation() {
        val rules = SduiValidationRules(maxLength = 5)
        assertEquals("Maximum length is 5 characters", SduiValidationResolver.validate("123456", rules))
        assertNull(SduiValidationResolver.validate("12345", rules))
    }

    @Test
    fun testRegexValidation() {
        val rules = SduiValidationRules(regex = "^[0-9]{10}$", errorMessage = "Invalid phone")
        assertEquals("Invalid phone", SduiValidationResolver.validate("12345", rules))
        assertNull(SduiValidationResolver.validate("9876543210", rules))
    }

    @Test
    fun testMalformedRegexDoesNotCrash() {
        val rules = SduiValidationRules(regex = "[abc")
        // Malformed regex should safely return null (ignore invalid rule) rather than crashing
        assertNull(SduiValidationResolver.validate("test", rules))
    }
}
