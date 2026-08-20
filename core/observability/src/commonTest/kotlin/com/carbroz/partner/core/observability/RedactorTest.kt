package com.carbroz.partner.core.observability

import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogValue
import com.carbroz.partner.core.observability.redaction.Redactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

class RedactorTest {

    @Test
    fun verifySensitiveKeyRedaction() {
        val sensitiveKeys = listOf(
            "authorization", "token", "access_token", "access-token", "accessToken",
            "refresh_token", "password", "userPassword", "otp", "pin", "secret",
            "api_key", "cookie", "set-cookie", "cvv", "cardNumber"
        )

        for (key in sensitiveKeys) {
            val input = mapOf(key to LogAttribute(LogValue.Text("raw_secret_value")))
            val sanitized = Redactor.sanitizeAttributes(input)
            assertEquals(
                LogValue.Text("[REDACTED_SECRET]"),
                sanitized[key],
                "Failed to redact sensitive key: $key"
            )
        }
    }

    @Test
    fun verifyPiiMaskingInText() {
        val emailMessage = "Contact user at test.user@carbroz.com for assistance"
        val phoneMessage = "Call customer at +919876543210 immediately"

        assertEquals(
            "Contact user at [REDACTED_EMAIL] for assistance",
            Redactor.sanitizeText(emailMessage)
        )
        assertEquals(
            "Call customer at [REDACTED_PHONE] immediately",
            Redactor.sanitizeText(phoneMessage)
        )
    }

    @Test
    fun verifyFreeTextCredentialSanitizationMatrix() {
        val cases = listOf(
            "Authorization: Bearer abc.def.ghi" to "Authorization: Bearer [REDACTED_SECRET]",
            "authorization=Bearer abc123" to "authorization=[REDACTED_SECRET]",
            "password=abc123" to "password=[REDACTED_SECRET]",
            "password: abc123" to "password=[REDACTED_SECRET]",
            "access_token=abc123" to "access_token=[REDACTED_SECRET]",
            "accessToken=abc123" to "access_token=[REDACTED_SECRET]",
            "refresh_token=abc123" to "refresh_token=[REDACTED_SECRET]",
            "refreshToken=abc123" to "refresh_token=[REDACTED_SECRET]",
            "api_key=abc123" to "api_key=[REDACTED_SECRET]",
            "apiKey=abc123" to "api_key=[REDACTED_SECRET]",
            "secret=abc123" to "secret=[REDACTED_SECRET]",
            "secret: abc123" to "secret=[REDACTED_SECRET]",
            "cookie=sessionId=abc123" to "cookie=[REDACTED_SECRET]",
            "set-cookie=sessionId=abc123" to "set-cookie=[REDACTED_SECRET]"
        )

        for ((input, expected) in cases) {
            assertEquals(expected, Redactor.sanitizeText(input), "Failed free-text redaction for input: $input")
        }
    }

    @Test
    fun verifyCredentialFalsePositivesPreserved() {
        val falsePositives = listOf(
            "token count is 42",
            "password policy enforced",
            "secret feature enabled"
        )

        for (text in falsePositives) {
            assertEquals(text, Redactor.sanitizeText(text), "False positive incorrectly redacted: $text")
        }
    }

    @Test
    fun verifyNonPhoneDiagnosticNumbersNotMasked() {
        val diagnosticStrings = listOf(
            "status=200",
            "duration=15000",
            "timestamp=1787000000000",
            "width=1920 height=1080",
            "version=20260818",
            "requestCount=1234567890",
            "value=-1234567890",
            "ratio=1234567890.123"
        )

        for (text in diagnosticStrings) {
            assertEquals(text, Redactor.sanitizeText(text), "Diagnostic string incorrectly redacted: $text")
        }
    }

    @Test
    fun verifyNestedStructureAndCollectionRedaction() {
        val nestedInput = mapOf(
            "user" to LogAttribute(
                LogValue.Structure(
                    mapOf(
                        "name" to LogAttribute(LogValue.Text("John Doe")),
                        "password" to LogAttribute(LogValue.Text("mySecret123"))
                    )
                )
            ),
            "tokens" to LogAttribute(
                LogValue.Collection(
                    listOf(
                        LogValue.Text("public_token"),
                        LogValue.Text("secret_pass_123")
                    )
                )
            )
        )

        val sanitized = Redactor.sanitizeAttributes(nestedInput)

        val userStruct = sanitized["user"] as LogValue.Structure
        assertEquals(
            LogValue.Text("[REDACTED_SECRET]"),
            (userStruct.attributes["password"] as LogAttribute).value
        )
    }

    @Test
    fun verifyHardenedThrowableSanitization() {
        val cause = IllegalStateException("Cause user@example.com accessToken=xyz refresh_token=refresh123")
        val exception = RuntimeException("Authorization: Bearer abc123 password=secret123", cause)

        val errorInfo = Redactor.sanitizeThrowable(exception)

        assertNotNull(errorInfo)
        assertEquals("RuntimeException", errorInfo.type)
        assertEquals("Authorization: Bearer [REDACTED_SECRET] password=[REDACTED_SECRET]", errorInfo.message)
        assertEquals("IllegalStateException", errorInfo.causeType)
        assertEquals("Cause [REDACTED_EMAIL] access_token=[REDACTED_SECRET] refresh_token=[REDACTED_SECRET]", errorInfo.causeMessage)

        assertFalse(errorInfo.message!!.contains("abc123"))
        assertFalse(errorInfo.message!!.contains("secret123"))
        assertFalse(errorInfo.causeMessage!!.contains("user@example.com"))
        assertFalse(errorInfo.causeMessage!!.contains("xyz"))
        assertFalse(errorInfo.causeMessage!!.contains("refresh123"))
    }
}
