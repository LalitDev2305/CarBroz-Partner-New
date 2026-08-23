package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carbroz.foundation.adaptive.AdaptiveLayoutProvider
import com.carbroz.foundation.designsystem.CarBrozTheme

/**
 * Temporary product shell used only to prove the fresh multiplatform build.
 *
 * The composition root intentionally consumes the shared design-system and
 * adaptive environment so Android, iOS and Desktop exercise the same Compose
 * path. It remains temporary until the static splash/reference vertical slice
 * replaces it in the later architecture phase.
 */
@Composable
fun CarBrozApp() {
    CarBrozTheme {
        AdaptiveLayoutProvider {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("CarBroz Partner")
            }
        }
    }
}
