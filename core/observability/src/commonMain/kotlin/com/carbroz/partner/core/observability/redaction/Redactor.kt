package com.carbroz.partner.core.observability.redaction

import com.carbroz.partner.core.observability.model.ErrorInfo
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogValue

/**
 * Single-owner security redaction engine enforcing credential masking, PII redaction, and attribute sanitization.
 */
internal object Redactor {

    private val SENSITIVE_KEYS = setOf(
        "authorization", "token", "accesstoken", "access_token", "access-token",
        "refreshtoken", "refresh_token", "refresh-token", "password", "userpassword",
        "otp", "pin", "secret", "sessionsecret", "clientsecret", "privatekey",
        "api_key", "apikey", "cookie", "setcookie", "set-cookie", "cvv", "cardnumber"
    )

    private val PII_EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val PII_PHONE_REGEX = Regex("(?:\\+91|\\+1|\\+44|\\+33|\\+49|\\+81|\\+86|\\+61)\\d{8,12}|(?:\\bphone\\b|\\bmobile\\b|\\bcontact\\b)\\s*[:=]?\\s*\\+?\\d{10,12}", RegexOption.IGNORE_CASE)

    private val CREDENTIAL_PATTERNS = listOf(
        Regex("(?i)\\bauthorization\\s*:\\s*bearer\\s+[^\\s,;\"]+") to "Authorization: Bearer [REDACTED_SECRET]",
        Regex("(?i)\\bauthorization\\s*=\\s*(?:bearer\\s+)?[^\\s,;\"]+") to "authorization=[REDACTED_SECRET]",
        Regex("(?i)\\bpassword\\s*[:=]\\s*[^\\s,;\"]+") to "password=[REDACTED_SECRET]",
        Regex("(?i)\\baccess[_-]?token\\s*[:=]\\s*[^\\s,;\"]+") to "access_token=[REDACTED_SECRET]",
        Regex("(?i)\\brefresh[_-]?token\\s*[:=]\\s*[^\\s,;\"]+") to "refresh_token=[REDACTED_SECRET]",
        Regex("(?i)\\bapi[_-]?key\\s*[:=]\\s*[^\\s,;\"]+") to "api_key=[REDACTED_SECRET]",
        Regex("(?i)\\bsecret\\s*[:=]\\s*[^\\s,;\"]+") to "secret=[REDACTED_SECRET]",
        Regex("(?i)\\bset[_-]?cookie\\s*[:=]\\s*[^\\s,;\"]+") to "set-cookie=[REDACTED_SECRET]",
        Regex("(?i)\\bcookie\\s*[:=]\\s*[^\\s,;\"]+") to "cookie=[REDACTED_SECRET]"
    )

    fun sanitizeAttributes(attributes: Map<String, LogAttribute>): Map<String, LogValue> {
        val result = LinkedHashMap<String, LogValue>(attributes.size)
        for ((key, attr) in attributes) {
            result[key] = if (isKeySensitive(key)) {
                LogValue.Text("[REDACTED_SECRET]")
            } else {
                sanitizeValue(attr.value)
            }
        }
        return result
    }

    fun sanitizeText(text: String?): String? {
        if (text == null) return null
        var sanitized = PII_EMAIL_REGEX.replace(text, "[REDACTED_EMAIL]")
        sanitized = PII_PHONE_REGEX.replace(sanitized, "[REDACTED_PHONE]")
        for ((pattern, replacement) in CREDENTIAL_PATTERNS) {
            sanitized = pattern.replace(sanitized, replacement)
        }
        return sanitized
    }

    fun sanitizeThrowable(t: Throwable?): ErrorInfo? {
        if (t == null) return null
        val cause = t.cause
        return ErrorInfo(
            type = t::class.simpleName ?: "Throwable",
            message = sanitizeText(t.message),
            causeType = cause?.let { it::class.simpleName ?: "Throwable" },
            causeMessage = cause?.let { sanitizeText(it.message) }
        )
    }

    fun isKeySensitive(key: String): Boolean {
        val normalized = key.lowercase().replace("_", "").replace("-", "")
        return SENSITIVE_KEYS.any { normalized.contains(it) }
    }

    private fun sanitizeValue(value: LogValue): LogValue = when (value) {
        is LogValue.Text -> LogValue.Text(sanitizeText(value.value) ?: "")
        is LogValue.Structure -> LogValue.Structure(
            value.attributes.mapValues { (k, v) ->
                if (isKeySensitive(k)) {
                    LogAttribute(LogValue.Text("[REDACTED_SECRET]"), v.sensitivity)
                } else {
                    LogAttribute(sanitizeValue(v.value), v.sensitivity)
                }
            }
        )
        is LogValue.Collection -> LogValue.Collection(
            value.items.map { sanitizeValue(it) }
        )
        else -> value
    }
}
