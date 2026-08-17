package com.carbroz.partner.app.composition

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Root multiplatform Compose entry point for CarBroz Partner.
 *
 * Owned by the application composition root ([com.carbroz.partner.app.composition]),
 * it serves as the single multiplatform rendering root invoked by platform hosts
 * (Android [com.carbroz.partner.MainActivity], Desktop [com.carbroz.partner.main],
 * and iOS [com.carbroz.partner.app.composition.MainViewController]).
 */
@Composable
fun CarBrozPartnerRoot() {
    MaterialTheme {
        Text(text = "CarBroz Partner")
    }
}
