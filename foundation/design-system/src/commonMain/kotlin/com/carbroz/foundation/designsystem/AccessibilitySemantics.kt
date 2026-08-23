package com.carbroz.foundation.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Shared Compose semantics helpers for renderer implementations.
 *
 * These helpers only apply accessibility metadata to already-resolved UI.
 * They do not introduce a parallel semantic content model beside SDUI.
 */
fun Modifier.accessibilityHeading(): Modifier = semantics { heading() }

fun Modifier.accessibilityDescription(description: String): Modifier {
    require(description.isNotBlank()) { "Accessibility description must not be blank." }
    return semantics { contentDescription = description }
}

fun Modifier.accessibilityRole(roleValue: Role): Modifier = semantics {
    role = roleValue
}
