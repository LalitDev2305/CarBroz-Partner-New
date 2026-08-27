package com.carbroz.feature.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SplashTeal = Color(0xFF08AFA8)
private val SplashTealLight = Color(0xFF9EE8E5)
private val SplashCyan = Color(0xFFE7F8F8)
private val SplashInk = Color(0xFF101522)
private val SplashBody = Color(0xFF565A62)

/** Native process-entry UI. Backend screen composition starts only after this feature completes. */
@Composable
fun SplashScreen(
    state: SplashState,
    onRetry: () -> Unit,
    onUpdateRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        SplashBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CarWashMark(Modifier.size(width = 128.dp, height = 92.dp))
            Spacer(Modifier.height(8.dp))
            BrandWordmark()
            PartnerWordmark()
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Premium Car Care\nAt Your Doorstep",
                color = SplashBody,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            PartnerCarArtwork(Modifier.fillMaxWidth(0.92f).aspectRatio(1.72f))
            Spacer(Modifier.height(18.dp))
            StartupStatus(state, onRetry, onUpdateRequested)
        }
    }
}

@Composable
private fun StartupStatus(
    state: SplashState,
    onRetry: () -> Unit,
    onUpdateRequested: (String) -> Unit,
) {
    when (state.phase) {
        SplashPhase.FORCE_UPDATE -> {
            val update = state.forceUpdate?.policy
            StartupBlockingCard(
                title = state.statusTitle,
                message = state.statusMessage,
                actionLabel = "Update CarBroz Partner",
                onAction = update?.storeUrl?.let { url -> { onUpdateRequested(url) } },
            )
        }
        SplashPhase.MAINTENANCE -> StartupBlockingCard(
            title = state.statusTitle,
            message = state.statusMessage,
            actionLabel = "Try again",
            onAction = onRetry,
        )
        SplashPhase.FAILED -> StartupBlockingCard(
            title = state.statusTitle,
            message = if (state.failure?.failure?.recoverable == true) {
                "We couldn't connect to CarBroz right now. Check your connection and try again."
            } else {
                "CarBroz couldn't safely finish startup. Please restart the app or contact support."
            },
            actionLabel = "Retry",
            onAction = if (state.failure?.failure?.recoverable == true) onRetry else null,
        )
        else -> {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.78f).height(6.dp),
                color = SplashTeal,
                trackColor = SplashTealLight.copy(alpha = 0.48f),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = state.statusTitle,
                color = SplashTeal,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.statusMessage,
                color = SplashBody,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StartupBlockingCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = SplashInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = message,
                color = SplashBody,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = SplashTeal),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(actionLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BrandWordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Car", color = SplashInk, fontSize = 50.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
        Text("Broz", color = SplashTeal, fontSize = 50.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
    }
}

@Composable
private fun PartnerWordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(54.dp).height(2.dp)) { drawRect(SplashTeal) }
        Spacer(Modifier.width(13.dp))
        Text("P A R T N E R", color = SplashTeal, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(13.dp))
        Canvas(Modifier.width(54.dp).height(2.dp)) { drawRect(SplashTeal) }
    }
}

@Composable
private fun SplashBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, SplashCyan.copy(alpha = 0.66f), Color.White),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(SplashCyan.copy(alpha = 0.95f), size.minDimension * 0.22f, Offset.Zero)
        drawCircle(
            SplashTealLight.copy(alpha = 0.32f),
            size.minDimension * 0.11f,
            Offset(size.width, size.height * 0.20f),
        )

        val lowerWave = Path().apply {
            moveTo(0f, size.height * 0.57f)
            cubicTo(
                size.width * 0.22f, size.height * 0.63f,
                size.width * 0.33f, size.height * 0.79f,
                size.width * 0.58f, size.height * 0.70f,
            )
            cubicTo(
                size.width * 0.80f, size.height * 0.62f,
                size.width * 0.88f, size.height * 0.48f,
                size.width, size.height * 0.39f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            lowerWave,
            Brush.linearGradient(
                listOf(SplashTealLight.copy(alpha = 0.55f), SplashCyan.copy(alpha = 0.60f), Color.White),
                Offset(0f, size.height * 0.63f),
                Offset(size.width, size.height),
            ),
        )
        listOf(
            Offset(size.width * 0.12f, size.height * 0.28f) to size.minDimension * 0.025f,
            Offset(size.width * 0.88f, size.height * 0.51f) to size.minDimension * 0.025f,
            Offset(size.width * 0.09f, size.height * 0.63f) to size.minDimension * 0.016f,
        ).forEach { (center, radius) ->
            drawCircle(SplashTeal.copy(alpha = 0.20f), radius, center)
            drawCircle(SplashTealLight.copy(alpha = 0.34f), radius * 0.62f, center + Offset(radius * 0.4f, -radius * 0.3f))
        }
    }
}

@Composable
private fun CarWashMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bodyTop = size.height * 0.53f
        drawRoundRect(
            color = SplashTeal,
            topLeft = Offset(size.width * 0.17f, bodyTop),
            size = Size(size.width * 0.66f, size.height * 0.30f),
            cornerRadius = CornerRadius(size.height * 0.09f),
        )
        drawRoundRect(Color.White, Offset(size.width * 0.24f, size.height * 0.61f), Size(size.width * 0.12f, size.height * 0.05f), CornerRadius(size.height * 0.02f))
        drawRoundRect(Color.White, Offset(size.width * 0.64f, size.height * 0.61f), Size(size.width * 0.12f, size.height * 0.05f), CornerRadius(size.height * 0.02f))
        drawRoundRect(Color.White, Offset(size.width * 0.41f, size.height * 0.73f), Size(size.width * 0.18f, size.height * 0.025f), CornerRadius(size.height * 0.01f))

        listOf(
            Offset(size.width * 0.35f, size.height * 0.43f) to size.height * 0.16f,
            Offset(size.width * 0.49f, size.height * 0.34f) to size.height * 0.20f,
            Offset(size.width * 0.63f, size.height * 0.43f) to size.height * 0.15f,
        ).forEach { (center, radius) ->
            drawCircle(Color.White, radius, center)
            drawCircle(SplashTeal, radius, center, style = Stroke(size.height * 0.035f))
        }
        drawRect(Color.White, Offset(size.width * 0.27f, size.height * 0.44f), Size(size.width * 0.46f, size.height * 0.14f))
        drawCircle(SplashTeal, size.height * 0.035f, Offset(size.width * 0.18f, size.height * 0.28f))
        drawCircle(SplashTeal, size.height * 0.025f, Offset(size.width * 0.75f, size.height * 0.32f))
        drawCircle(SplashTeal, size.height * 0.020f, Offset(size.width * 0.63f, size.height * 0.16f), style = Stroke(size.height * 0.025f))
    }
}

@Composable
private fun PartnerCarArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawOval(SplashTeal.copy(alpha = 0.14f), Offset(w * 0.08f, h * 0.69f), Size(w * 0.84f, h * 0.20f))

        val body = Path().apply {
            moveTo(w * 0.10f, h * 0.61f)
            cubicTo(w * 0.16f, h * 0.48f, w * 0.23f, h * 0.41f, w * 0.36f, h * 0.38f)
            cubicTo(w * 0.45f, h * 0.24f, w * 0.57f, h * 0.19f, w * 0.73f, h * 0.24f)
            cubicTo(w * 0.80f, h * 0.29f, w * 0.84f, h * 0.40f, w * 0.88f, h * 0.49f)
            cubicTo(w * 0.94f, h * 0.52f, w * 0.96f, h * 0.60f, w * 0.94f, h * 0.68f)
            lineTo(w * 0.86f, h * 0.72f)
            lineTo(w * 0.17f, h * 0.72f)
            cubicTo(w * 0.11f, h * 0.70f, w * 0.08f, h * 0.67f, w * 0.10f, h * 0.61f)
            close()
        }
        drawPath(body, Color(0xFFF9FCFD))
        drawPath(body, Color(0xFFB8CFD2), style = Stroke(h * 0.012f))

        val windows = Path().apply {
            moveTo(w * 0.38f, h * 0.39f)
            cubicTo(w * 0.46f, h * 0.27f, w * 0.57f, h * 0.24f, w * 0.70f, h * 0.29f)
            lineTo(w * 0.79f, h * 0.43f)
            lineTo(w * 0.39f, h * 0.43f)
            close()
        }
        drawPath(
            windows,
            Brush.linearGradient(
                listOf(Color(0xFF12262A), Color(0xFF3D5E63)),
                Offset(w * 0.42f, h * 0.28f),
                Offset(w * 0.75f, h * 0.44f),
            ),
        )
        drawLine(Color(0xFFAFC7CA), Offset(w * 0.57f, h * 0.27f), Offset(w * 0.56f, h * 0.43f), h * 0.009f)
        drawRoundRect(Color(0xFF172126), Offset(w * 0.23f, h * 0.57f), Size(w * 0.50f, h * 0.12f), CornerRadius(h * 0.025f))
        repeat(5) { index ->
            val x = w * (0.30f + index * 0.085f)
            drawLine(Color(0xFF516067), Offset(x, h * 0.59f), Offset(x, h * 0.67f), h * 0.006f)
        }
        drawRoundRect(Color(0xFF11171A), Offset(w * 0.42f, h * 0.66f), Size(w * 0.18f, h * 0.075f), CornerRadius(h * 0.012f))
        drawLine(SplashTeal, Offset(w * 0.44f, h * 0.70f), Offset(w * 0.58f, h * 0.70f), h * 0.008f)

        listOf(w * 0.24f, w * 0.79f).forEach { x ->
            drawCircle(Color(0xFF182126), h * 0.13f, Offset(x, h * 0.72f))
            drawCircle(Color(0xFF607479), h * 0.072f, Offset(x, h * 0.72f))
            drawCircle(Color(0xFF16282B), h * 0.040f, Offset(x, h * 0.72f))
        }
        drawRoundRect(Color(0xFFDFFBFA), Offset(w * 0.13f, h * 0.53f), Size(w * 0.13f, h * 0.055f), CornerRadius(h * 0.02f))
        drawRoundRect(Color(0xFFDFFBFA), Offset(w * 0.75f, h * 0.52f), Size(w * 0.12f, h * 0.055f), CornerRadius(h * 0.02f))
    }
}
