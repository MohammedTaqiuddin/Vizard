package com.example.algovisualizer.features.queue.components

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.features.queue.NodeState
import com.example.algovisualizer.features.queue.QueueNode
import com.example.algovisualizer.features.queue.QueueViewModel
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class FallingNode(
    val initialX: Float,
    val yOffset: Animatable<Float, *>,
    val alpha: Animatable<Float, *>,
    val value: Int,
)

@Composable
fun QueueVisualizer(viewModel: QueueViewModel) {
    val queue by viewModel.queue.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val nodeWidth: Float
        val nodeHeight: Float
        val nodeSpacing: Float
        val startX: Float
        val cornerRadius: Float
        val textSize: Float

        with(LocalDensity.current) {
            nodeWidth = 50.dp.toPx()
            nodeHeight = 50.dp.toPx()
            nodeSpacing = 12.dp.toPx()
            startX = nodeWidth / 2 + nodeSpacing
            cornerRadius = 8.dp.toPx()
            textSize = 18.dp.toPx()
        }

        val animatedOffsets = remember { mutableMapOf<UUID, Animatable<Float, *>>() }
        val fallingNodes = remember { mutableMapOf<UUID, FallingNode>() }
        var prevQueue by remember { mutableStateOf<List<QueueNode>>(emptyList()) }
        val canvasHeightPx = constraints.maxHeight
        val canvasWidth = constraints.maxWidth.toFloat()
        val hasLaidOut = remember(constraints.maxWidth) { constraints.maxWidth > 0 }

        LaunchedEffect(queue, hasLaidOut) {
            if (!hasLaidOut) return@LaunchedEffect

            val currentQueueIds = queue.map { it.id }.toSet()
            val prevQueueIds = prevQueue.map { it.id }.toSet()
            val isDequeue = prevQueue.size > queue.size

            coroutineScope {
                // 1. Handle dequeued nodes
                val dequeuedNodes = prevQueue.filter { it.id !in currentQueueIds }
                dequeuedNodes.forEach { node ->
                    val lastKnownOffset = animatedOffsets[node.id]?.value
                    if (lastKnownOffset != null) {
                        val anims = FallingNode(
                            initialX = lastKnownOffset,
                            yOffset = Animatable(canvasHeightPx / 2f),
                            alpha = Animatable(1f),
                            value = node.value
                        )
                        fallingNodes[node.id] = anims
                        animatedOffsets.remove(node.id) // Handoff to falling animation

                        launch {
                            anims.yOffset.animateTo(
                                targetValue = canvasHeightPx.toFloat() + nodeHeight,
                                animationSpec = tween(600)
                            )
                            fallingNodes.remove(node.id) // Clean up after animation
                        }
                        launch {
                            anims.alpha.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(600)
                            )
                        }
                    }
                }

                // 2. Handle existing and new nodes
                queue.forEachIndexed { index, node ->
                    val targetX = startX + index * (nodeWidth + nodeSpacing)
                    val isNewNode = node.id !in prevQueueIds

                    val animatable = animatedOffsets.getOrPut(node.id) {
                        Animatable(canvasWidth + nodeWidth)
                    }

                    launch {
                        animatable.animateTo(
                            targetValue = targetX,
                            animationSpec = if (isNewNode) {
                                tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                            } else if (isDequeue) {
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            } else {
                                tween(300)
                            }
                        )
                    }
                }
            }
            prevQueue = queue
        }

        val textPaint = remember(textSize) {
            AndroidPaint().apply {
                isAntiAlias = true
                this.textSize = textSize
                color = android.graphics.Color.WHITE
                textAlign = AndroidPaint.Align.CENTER
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f

            queue.forEach { node ->
                val offsetX = animatedOffsets[node.id]?.value
                if (offsetX != null) {
                    val nodeColor = when (nodeStates[node.id]) {
                        NodeState.ENQUEUED -> Color.Green
                        else -> Color.Blue
                    }
                    drawRoundRect(
                        color = nodeColor,
                        topLeft = Offset(offsetX - nodeWidth / 2, centerY - nodeHeight / 2),
                        size = Size(nodeWidth, nodeHeight),
                        cornerRadius = CornerRadius(cornerRadius)
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            node.value.toString(),
                            offsetX,
                            centerY - (textPaint.ascent() + textPaint.descent()) / 2,
                            textPaint
                        )
                    }
                }
            }

            fallingNodes.forEach { (_, anims) ->
                val fallingY = anims.yOffset.value
                val currentAlpha = anims.alpha.value
                val offsetX = anims.initialX

                val fallingTextPaint = AndroidPaint(textPaint).apply {
                    alpha = (255 * currentAlpha).toInt()
                }

                drawRoundRect(
                    color = Color.Red.copy(alpha = currentAlpha),
                    topLeft = Offset(offsetX - nodeWidth / 2, fallingY - nodeHeight / 2),
                    size = Size(nodeWidth, nodeHeight),
                    cornerRadius = CornerRadius(cornerRadius)
                )

                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        anims.value.toString(),
                        offsetX,
                        fallingY - (fallingTextPaint.ascent() + fallingTextPaint.descent()) / 2,
                        fallingTextPaint
                    )
                }
            }
        }
    }
}
