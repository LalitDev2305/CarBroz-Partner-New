package com.carbroz.partner.core.observability

import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogValue
import com.carbroz.partner.core.observability.redaction.Redactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
    fun verifyFreeTextCredentialSanitization() {
        val authHeaderMsg = "Header Authorization: Bearer secret_token_xyz"
        val passMsg = "Login failed with password=mySecretPassword123"
        val tokenMsg = "Received access_token=jwt_token_val_456"

        assertEquals(
            "Header Authorization: Bearer [REDACTED_SECRET]",
            Redactor.sanitizeText(authHeaderMsg)
        )
        assertEquals(
            "Login failed with password=[REDACTED_SECRET]",
            Redactor.sanitizeText(passMsg)
        )
        assertEquals(
            "Received access_token=[REDACTED_SECRET]",
            Redactor.sanitizeText(tokenMsg)
        )
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
    fun verifyThrowableSanitization() {
        val exception = RuntimeException("Failed connect to user@carbroz.com with password=secret123", IllegalStateException("Root cause phone: +919876543210"))

        val errorInfo = Redactor.sanitizeThrowable(exception)

        assertNotNull(errorInfo)
        assertEquals("RuntimeException", errorInfo.type)
        assertEquals("Failed connect to [REDACTED_EMAIL] with password=[REDACTED_SECRET]", errorInfo.message)
        assertEquals("IllegalStateException", errorInfo.causeType)
        assertEquals("Root cause [REDACTED_PHONE]", errorInfo.causeMessage)
    }
}
