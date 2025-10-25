package com.example.algovisualizer.features.graph.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.features.graph.GraphNodeState
import com.example.algovisualizer.features.graph.GraphViewModel
import com.example.algovisualizer.features.graph.InteractionMode
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun GraphVisualizer(
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier,
) {
    val nodes by viewModel.nodes.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()
    val currentMode by viewModel.interactionMode.collectAsState()
    val scope = rememberCoroutineScope()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (currentMode == InteractionMode.PAN_ZOOM) {
            scale *= zoomChange
            offset += panChange
        }
    }

    val nodeRadius = 24.dp

    val animatedNodeColors = remember { mutableStateMapOf<String, Color>() }
    nodes.values.forEach { node ->
        val targetColor = when (nodeStates[node.id]) {
            GraphNodeState.VISITED -> Color(0xFF4DB6AC)
            GraphNodeState.IN_QUEUE -> Color(0xFF4FC3F7)
            GraphNodeState.CURRENT -> Color(0xFFFFA500)
            GraphNodeState.PATH -> Color.Green
            GraphNodeState.SELECTED -> Color.Red // Highlight for selection
            else -> Color.Blue
        }
        val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = spring(stiffness = 50f),
            label = "NodeColorAnimation-${node.id}"
        )
        animatedNodeColors[node.id] = animatedColor
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentMode) { // Re-trigger pointer input when mode changes
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val worldOffset = (tapOffset - offset) / scale
                            scope.launch {
                                when (currentMode) {
                                    InteractionMode.ADD_NODE -> {
                                        viewModel.onOperation("add_node", worldOffset)
                                    }

                                    InteractionMode.ADD_EDGE -> {
                                        val tappedNode = nodes.values.find {
                                            val distance = sqrt(
                                                (worldOffset.x - it.offset.x).pow(2) +
                                                        (worldOffset.y - it.offset.y).pow(2)
                                            )
                                            distance < nodeRadius.toPx()
                                        }
                                        if (tappedNode != null) {
                                            viewModel.onOperation("select_node", tappedNode.id)
                                        } else {
                                            // Tapped on empty space, cancel selection
                                            viewModel.onOperation("reset")
                                        }
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    )
                }
                .transformable(state = transformableState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            // 1. Draw edges
            edges.forEach { edge ->
                val fromNode = nodes[edge.from]
                val toNode = nodes[edge.to]
                if (fromNode != null && toNode != null) {
                    drawLine(
                        color = Color.Gray,
                        start = fromNode.offset,
                        end = toNode.offset,
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // 2. Draw nodes
            nodes.values.forEach { node ->
                val nodeColor = animatedNodeColors[node.id] ?: when (nodeStates[node.id]) {
                    GraphNodeState.VISITED -> Color(0xFF4DB6AC)
                    GraphNodeState.IN_QUEUE -> Color(0xFF4FC3F7)
                    GraphNodeState.CURRENT -> Color(0xFFFFA500)
                    GraphNodeState.PATH -> Color.Green
                    GraphNodeState.SELECTED -> Color.Red // Highlight for selection
                    else -> Color.Blue
                }

                drawCircle(
                    color = nodeColor,
                    radius = nodeRadius.toPx(),
                    center = node.offset
                )

                drawIntoCanvas {
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 18.dp.toPx()
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    it.nativeCanvas.drawText(
                        node.label,
                        node.offset.x,
                        node.offset.y - (paint.ascent() + paint.descent()) / 2,
                        paint
                    )
                }
            }
        }
    }
}
