package com.carbroz.partner.sdui.runtime.validation

import com.carbroz.partner.sdui.engine.model.SduiValidationRules

/**
 * Pure, stateless validation resolver evaluating [SduiValidationRules] against runtime string inputs.
 */
public object SduiValidationResolver {
    public fun validate(
        value: String,
        rules: SduiValidationRules
    ): String? {
        val trimmed = value.trim()
        if (rules.required && trimmed.isEmpty()) {
            return rules.errorMessage ?: "This field is required"
        }
        val minLen = rules.minLength
        if (trimmed.isNotEmpty() && minLen != null && trimmed.length < minLen) {
            return rules.errorMessage ?: "Minimum length is $minLen characters"
        }
        val maxLen = rules.maxLength
        if (trimmed.isNotEmpty() && maxLen != null && trimmed.length > maxLen) {
            return rules.errorMessage ?: "Maximum length is $maxLen characters"
        }
        val regexStr = rules.regex
        if (trimmed.isNotEmpty() && !regexStr.isNullOrBlank()) {
            try {
                val pattern = Regex(regexStr)
                if (!pattern.matches(trimmed)) {
                    return rules.errorMessage ?: "Invalid input format"
                }
            } catch (_: Exception) {
                // Invalid regex from backend MUST NOT crash runtime; safely ignore malformed rule
                return null
            }
        }
        return null
    }
}
