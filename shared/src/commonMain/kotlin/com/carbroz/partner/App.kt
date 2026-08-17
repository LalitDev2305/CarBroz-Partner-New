package com.carbroz.partner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Common Compose root exposed to each platform host.
 *
 * Platform entry points (`MainActivity` on Android, `MainViewController` on iOS,
 * and `main` on Desktop) delegate UI rendering ownership to this function,
 * ensuring that the core bootstrap UI remains shared and identical across all targets.
 */
@Composable
fun CarBrozPartnerRoot() {
    Text(text = "CarBroz Partner")
}

