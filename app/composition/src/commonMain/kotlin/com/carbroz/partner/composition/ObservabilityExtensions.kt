package com.carbroz.partner.composition

import com.carbroz.foundation.observability.CrashEvent
import com.carbroz.foundation.observability.DiagnosticAttribute
import com.carbroz.foundation.observability.Observability

internal fun Observability.recordNonFatal(throwable: Throwable, attributes: Map<String, String>) {
    crash(
        event = CrashEvent(
            category = "non_fatal",
            message = throwable.message ?: throwable::class.simpleName ?: "non_fatal",
            attributes = attributes.mapValues { DiagnosticAttribute(it.value) },
        ),
        throwable = throwable,
    )
}
