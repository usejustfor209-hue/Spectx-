package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlowBlue
import com.example.ui.theme.GlowPurple

@Composable
fun AnimatedVoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float = 0f,
    modifier: Modifier = Modifier,
    orbSize: Dp = 140.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val activeScale = when {
        isListening -> (1.0f + rmsDb * 0.4f).coerceIn(1.0f, 1.45f)
        isSpeaking -> pulseScale
        else -> 1.0f
    }

    val primaryColor = when {
        isListening -> Color(0xFFEF4444) // Vibrant active red/coral
        isSpeaking -> GlowBlue
        else -> GlowPurple
    }

    val secondaryColor = when {
        isListening -> Color(0xFFF59E0B)
        isSpeaking -> Color(0xFF06B6D4)
        else -> Color(0xFF3B82F6)
    }

    Box(
        modifier = modifier
            .size(orbSize * 1.5f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = orbSize / 1.5f),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(orbSize * 1.5f)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (orbSize.toPx() / 2f) * activeScale

            if (isListening || isSpeaking) {
                // Outer wave 1
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = haloAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.4f
                    ),
                    radius = baseRadius * 1.4f,
                    center = center
                )

                // Outer wave 2
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = haloAlpha * 0.7f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.2f
                    ),
                    radius = baseRadius * 1.2f,
                    center = center
                )
            }

            // Core glowing gradient orb
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    start = Offset(center.x - baseRadius, center.y - baseRadius),
                    end = Offset(center.x + baseRadius, center.y + baseRadius)
                ),
                radius = baseRadius * 0.85f,
                center = center
            )
        }

        // Inner icon indicator
        Box(
            modifier = Modifier
                .size(orbSize * 0.7f)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isListening -> Icons.Default.Mic
                    isSpeaking -> Icons.Default.VolumeUp
                    else -> Icons.Default.GraphicEq
                },
                contentDescription = when {
                    isListening -> "Listening"
                    isSpeaking -> "Speaking"
                    else -> "Voice assistant idle"
                },
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
