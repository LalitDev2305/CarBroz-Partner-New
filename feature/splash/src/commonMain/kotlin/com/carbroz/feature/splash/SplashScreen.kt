package com.carbroz.feature.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carbroz.foundation.designsystem.CarBrozDesignSystem
import com.carbroz.foundation.designsystem.carBrozSafeDrawingPadding

/** Temporary static process-entry UI. Final visual design can change without changing startup architecture. */
@Composable
fun SplashScreen(
    state: SplashState,
    onRetry: () -> Unit,
    onUpdateRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CarBrozDesignSystem.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .carBrozSafeDrawingPadding()
            .padding(horizontal = spacing.large, vertical = spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "CarBroz Partner",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.small))
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.small))
        Text(
            text = "Doorstep car care, ready when you are.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.extraLarge))

        when (state) {
            SplashState.Loading,
            SplashState.Ready,
            -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(spacing.medium))
                Text(
                    text = "Preparing your partner experience…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            is SplashState.RequiredUpdate -> BlockingContent(
                title = state.title,
                message = state.message,
                actionLabel = "Update CarBroz Partner",
                onAction = onUpdateRequested,
            )

            is SplashState.Maintenance -> BlockingContent(
                title = state.title,
                message = state.message,
                actionLabel = "Try again",
                onAction = onRetry.takeIf { state.retryEnabled },
            )

            is SplashState.Error -> BlockingContent(
                title = "Unable to start",
                message = state.message,
                actionLabel = "Retry",
                onAction = onRetry.takeIf { state.retryEnabled },
            )
        }
    }
}

@Composable
private fun BlockingContent(
    title: String,
    message: String,
    actionLabel: String,
    onAction: (() -> Unit)?,
) {
    val spacing = CarBrozDesignSystem.spacing
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onAction != null) {
            Spacer(Modifier.height(spacing.large))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
