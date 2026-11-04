package com.example.algovisualizer.features.linkedlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.core.ui.glass.GlassIconButton
import com.example.algovisualizer.core.ui.glass.GlassSurface
import com.example.algovisualizer.features.linkedlist.core.SPEEDS
import com.example.algovisualizer.features.linkedlist.core.Timeline

/**
 * Transport for a step trace (plan 12.2): restart, step back, play/pause, step forward,
 * finish, speed, and a scrubber with one tick per step. The narration for the current step
 * sits above it, so the explanation and the picture always agree.
 */
@Composable
fun PlaybackBar(
    timeline: Timeline,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onRestart: () -> Unit,
    onFinish: () -> Unit,
    onGoTo: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    val step = timeline.current ?: return

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        strong = true
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = step.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Dismiss trace",
                    onClick = onDismiss
                )
            }

            if (step.vars.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    step.vars.forEach { (name, value) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(glass.accentSoft)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$name = ${value ?: "NULL"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accent
                            )
                        }
                    }
                }
            }

            // Cost meter: the complexity lecture, counted live.
            val cost = timeline.cost()
            if (!cost.isEmpty) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOfNotNull(
                        cost.walked.takeIf { it > 0 }?.let { "$it walked" },
                        cost.pointerWrites.takeIf { it > 0 }?.let { "$it pointer writes" },
                        cost.nodes.takeIf { it > 0 }?.let { "$it nodes" }
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textTertiary
                )
            }

            Spacer(Modifier.height(10.dp))

            // Scrubber: one tick per step, filled up to the current one.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                timeline.steps.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i <= timeline.index) glass.accent else glass.fillStrong)
                            .clickable { onGoTo(i) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassIconButton(Icons.Default.Replay, "Restart", onRestart)
                GlassIconButton(Icons.Default.SkipPrevious, "Step back", onStepBack)
                GlassIconButton(
                    icon = if (timeline.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timeline.playing) "Pause" else "Play",
                    active = timeline.playing,
                    onClick = if (timeline.playing) onPause else onPlay
                )
                GlassIconButton(Icons.Default.SkipNext, "Step forward", onStepForward)
                GlassIconButton(Icons.Default.FastForward, "Finish", onFinish)

                Spacer(Modifier.weight(1f))

                Text(
                    text = "${timeline.index + 1}/${timeline.steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textTertiary
                )
                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(glass.fillStrong)
                        .clickable {
                            val i = SPEEDS.indexOf(timeline.speed).let { if (it < 0) 1 else it }
                            onSpeed(SPEEDS[(i + 1) % SPEEDS.size])
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${timeline.speed}x".replace(".0x", "x"),
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textPrimary
                    )
                }
            }
        }
    }
}
