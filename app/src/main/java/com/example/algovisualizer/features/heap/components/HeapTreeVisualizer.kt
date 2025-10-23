package com.example.algovisualizer.features.heap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.features.heap.HeapViewModel
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

@Composable
fun HeapTreeVisualizer(viewModel: HeapViewModel) {
    val heap by viewModel.heap.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // This is a flag to indicate if we have performed the initial centering.
    // We key it to the heap's identity to reset it when the heap is cleared and re-populated.
    var hasCentered by remember(heap) { mutableStateOf(false) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offset += panChange
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformableState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
        ) {
            if (heap.isEmpty()) {
                // If the heap is cleared, reset the state for the next visualization
                hasCentered = false
                scale = 1f
                offset = Offset.Zero
                return@Canvas
            }

            val nodeRadius = 24.dp.toPx()
            val levelHeight = 120.dp.toPx()
            // Fixed horizontal spacing for the most packed (leaf) level
            val leafHorizontalSpacing = nodeRadius * 3.5f

            val maxLevel = floor(log2(heap.size.toFloat())).toInt()
            // The total virtual width our tree layout will occupy
            val treeWidth = 2f.pow(maxLevel) * leafHorizontalSpacing

            fun getNodeOffsetInWorld(index: Int): Offset {
                val level = if (index == 0) 0 else floor(log2((index + 1).toFloat())).toInt()
                val y = (level * levelHeight)

                val nodesInLevel = 2f.pow(level).toInt()
                val indexInLevel = (index + 1) - nodesInLevel

                // The spacing between nodes at this particular level
                val spacingForLevel = treeWidth / nodesInLevel
                val x = indexInLevel * spacingForLevel + (spacingForLevel / 2)

                return Offset(x, y)
            }

            // Perform the initial centering translation only once
            if (!hasCentered) {
                val rootNodeWorldOffset = getNodeOffsetInWorld(0)
                // Calculate the offset needed to bring the root to the top-center of the screen
                val initialOffset = Offset(
                    x = this.size.center.x - rootNodeWorldOffset.x,
                    y = 150.dp.toPx() - rootNodeWorldOffset.y // Position root 150.dp from the top
                )
                offset = initialOffset
                hasCentered = true
            }

            // Draw connections
            heap.forEachIndexed { i, _ ->
                val parentOffset = getNodeOffsetInWorld(i)
                val leftChildIndex = 2 * i + 1
                val rightChildIndex = 2 * i + 2

                if (leftChildIndex < heap.size) {
                    val leftChildOffset = getNodeOffsetInWorld(leftChildIndex)
                    drawLine(Color.Gray, parentOffset, leftChildOffset, 2.dp.toPx())
                }
                if (rightChildIndex < heap.size) {
                    val rightChildOffset = getNodeOffsetInWorld(rightChildIndex)
                    drawLine(Color.Gray, parentOffset, rightChildOffset, 2.dp.toPx())
                }
            }

            // Draw nodes and text
            heap.forEachIndexed { index, value ->
                val nodeOffset = getNodeOffsetInWorld(index)

                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = 18.dp.toPx()
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val nodeColor = nodeStates[value]?.let {
                    when (it) {
                        com.example.algovisualizer.features.heap.NodeState.COMPARING -> Color.Yellow
                        com.example.algovisualizer.features.heap.NodeState.FOUND -> Color.Green
                        else -> Color.Blue
                    }
                } ?: Color.Blue

                drawCircle(nodeColor, nodeRadius, nodeOffset)

                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        value.toString(),
                        nodeOffset.x,
                        nodeOffset.y - (paint.ascent() + paint.descent()) / 2,
                        paint
                    )
                }
            }
        }
    }
}
