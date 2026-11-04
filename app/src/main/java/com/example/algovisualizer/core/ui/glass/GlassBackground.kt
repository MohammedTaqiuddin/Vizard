package com.example.algovisualizer.core.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.algovisualizer.core.theme.LocalGlass
import kotlin.math.cos
import kotlin.math.sin

/**
 * The ambient layer every screen sits on: a deep base wash with three slowly drifting colour
 * orbs. Glass surfaces read as glass because this is what shows through them.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlass.current
    val transition = rememberInfiniteTransition(label = "aurora")

    // One slow phase drives all three orbs on different radii, so they never line up twice.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(glass.base, glass.baseDeep))
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = minOf(w, h) * 0.78f

            fun orb(color: Color, cx: Float, cy: Float, scale: Float) {
                val radius = r * scale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = glass.orbAlpha),
                            color.copy(alpha = glass.orbAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }

            orb(
                glass.orbPrimary,
                w * (0.22f + 0.10f * cos(phase)),
                h * (0.18f + 0.06f * sin(phase)),
                0.95f
            )
            orb(
                glass.orbSecondary,
                w * (0.86f + 0.09f * sin(phase * 0.8f)),
                h * (0.34f + 0.08f * cos(phase * 1.3f)),
                0.80f
            )
            orb(
                glass.orbTertiary,
                w * (0.50f + 0.14f * cos(phase * 0.6f + 2f)),
                h * (0.88f + 0.05f * sin(phase * 0.9f)),
                1.05f
            )
        }
        content()
    }
}
