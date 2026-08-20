package com.carbroz.partner.core.observability

import com.carbroz.partner.core.observability.model.ErrorInfo
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.LogValue
import com.carbroz.partner.core.observability.model.TraceContext
import com.carbroz.partner.core.observability.sink.formatLogEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleLogSinkTest {

    @Test
    fun verifyConsoleLogSinkFormattingIncludesAllSanitizedFields() {
        val event = LogEvent(
            timestampMs = 1700000000000L,
            level = LogLevel.INFO,
            category = LogCategory.APP,
            sourceClass = "TestClass",
            sourceFunction = "testFunction",
            event = "user_login",
            message = "User [REDACTED_EMAIL] logged in",
            attributes = mapOf(
                "userId" to LogValue.Text("usr_123"),
                "password" to LogValue.Text("[REDACTED_SECRET]")
            ),
            traceContext = TraceContext(traceId = "trace_abc", screenId = "login_screen"),
            durationMs = 42L,
            errorInfo = ErrorInfo(type = "RuntimeException", message = "Sanitized error message")
        )

        val formatted = formatLogEvent(event)

        assertTrue(formatted.contains("[1700000000000]"))
        assertTrue(formatted.contains("[INFO]"))
        assertTrue(formatted.contains("[APP]"))
        assertTrue(formatted.contains("[TestClass::testFunction]"))
        assertTrue(formatted.contains("user_login: User [REDACTED_EMAIL] logged in"))
        assertTrue(formatted.contains("attributes="))
        assertTrue(formatted.contains("userId"))
        assertTrue(formatted.contains("[REDACTED_SECRET]"))
        assertTrue(formatted.contains("traceContext="))
        assertTrue(formatted.contains("trace_abc"))
        assertTrue(formatted.contains("durationMs=42"))
        assertTrue(formatted.contains("errorInfo="))
        assertTrue(formatted.contains("RuntimeException"))

        assertFalse(formatted.contains("user@carbroz.com"))
        assertFalse(formatted.contains("raw_secret"))
    }
}
