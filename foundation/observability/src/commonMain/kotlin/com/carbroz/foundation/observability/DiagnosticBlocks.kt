package com.carbroz.foundation.observability

/** Curated multiline diagnostic block kinds used only by local/staging operational adapters. */
enum class DiagnosticBlockKind {
    API_REQUEST,
    API_RESPONSE,
    API_ERROR,
}

/**
 * Presentation-ready diagnostic block for information that does not fit bounded structured attributes.
 *
 * Callers must sanitize sensitive values before constructing a block. Production composition supplies the
 * no-op sink, so these detailed blocks never become a production logging channel.
 */
data class DiagnosticBlock(
    val kind: DiagnosticBlockKind,
    val title: String,
    val content: String,
)

/** Receives already-sanitized local/staging multiline diagnostics. */
fun interface DiagnosticBlockSink {
    fun emit(block: DiagnosticBlock)
}

/** Shared no-op detailed diagnostic sink. */
val NoOpDiagnosticBlockSink: DiagnosticBlockSink = DiagnosticBlockSink {}
