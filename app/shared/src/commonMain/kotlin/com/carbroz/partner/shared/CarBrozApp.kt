package com.carbroz.partner.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Temporary product shell used only to prove the fresh multiplatform build.
 *
 * This composable will be replaced by the application runtime and static
 * Splash vertical slice as the next architecture phases are implemented.
 */
@Composable
fun CarBrozApp() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("CarBroz Partner")
        }
    }
}
