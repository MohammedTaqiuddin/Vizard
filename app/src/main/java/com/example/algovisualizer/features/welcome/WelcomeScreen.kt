package com.example.algovisualizer.features.welcome

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.algovisualizer.R
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.core.ui.glass.GlassButton

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    val glass = LocalGlass.current

    // First impression is a peak moment: the mark settles in rather than snapping on.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label = "reveal"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            // Accent glow behind the mark -- depth without a heavy container.
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .alpha(reveal * 0.55f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(glass.accent.copy(alpha = 0.30f), Color.Transparent)
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Vizard logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(0.90f + 0.10f * reveal)
                    .alpha(reveal)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Vizard",
            style = MaterialTheme.typography.displaySmall,
            color = glass.textPrimary,
            modifier = Modifier.alpha(reveal)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Watch data structures think",
            style = MaterialTheme.typography.bodyLarge,
            color = glass.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(reveal)
        )

        Spacer(Modifier.weight(1f))

        // Primary action lives in the thumb zone, not centred on the screen.
        GlassButton(
            text = "Start exploring",
            onClick = onStartClick,
            prominent = true,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(reveal)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "\"Unity can only be manifested by the Binary.\n" +
                "Unity itself and the idea of Unity are already two.\"",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = glass.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(reveal * 0.9f)
        )

        Spacer(Modifier.height(48.dp))
    }
}
