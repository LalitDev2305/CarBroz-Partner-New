package com.carbroz.foundation.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier

/**
 * Applies the platform safe-drawing insets to top-level application content.
 *
 * This is intentionally a Compose primitive rather than a platform abstraction:
 * Compose already owns the Android/iOS/Desktop inset implementation. Keeping the
 * policy here gives application and future SDUI renderers one canonical way to
 * avoid display cut-outs, system bars and other unsafe drawing regions without
 * duplicating platform-specific code.
 */
fun Modifier.carBrozSafeDrawingPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing)

/**
 * Keeps editable content clear of the on-screen keyboard when it is visible.
 *
 * Callers opt in at the container that owns editable content; applying IME
 * padding globally would unnecessarily resize screens that contain no input.
 */
fun Modifier.carBrozImePadding(): Modifier = imePadding()
