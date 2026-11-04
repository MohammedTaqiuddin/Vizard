package com.example.algovisualizer.features.bst

import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.core.ui.glass.GlassButton
import com.example.algovisualizer.core.ui.glass.GlassSegmented
import com.example.algovisualizer.core.ui.glass.GlassTextField
import com.example.algovisualizer.core.ui.glass.GlassVisualizerScreen
import kotlinx.coroutines.launch

private fun Color.darker(factor: Float = 0.7f): Color {
    return Color(red * factor, green * factor, blue * factor, alpha)
}

private fun Color.lighter(factor: Float = 1.4f): Color {
    return Color(
        (red * factor).coerceIn(0f, 1f),
        (green * factor).coerceIn(0f, 1f),
        (blue * factor).coerceIn(0f, 1f),
        alpha
    )
}

@Composable
fun BstScreen(
    onBackClick: () -> Unit,
    viewModel: BstViewModel = viewModel()
) {
    var inputValue by remember { mutableStateOf("") }
    var selectedTraversal by remember { mutableStateOf(TraversalType.IN_ORDER) }

    val glass = LocalGlass.current
    val traversalResult by viewModel.traversalResult.collectAsState()
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()
    val code by viewModel.pythonCode.collectAsState()
    val inOrder by viewModel.inOrderNodes.collectAsState()

    GlassVisualizerScreen(
        title = "Binary Search Tree",
        subtitle = if (inOrder.isEmpty()) "Insert a value to begin" else "${inOrder.size} nodes",
        onBack = onBackClick,
        code = code,
        highlightedLines = highlightedLines,
        canvasLabel = "Tree",
        canvasIcon = Icons.Default.AccountTree,
        controls = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = "Value",
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Insert",
                    prominent = true,
                    enabled = inputValue.toIntOrNull() != null,
                    onClick = {
                        inputValue.toIntOrNull()?.let {
                            viewModel.insertValue(it)
                            inputValue = ""
                        }
                    }
                )
                GlassButton(
                    text = "Find",
                    enabled = inputValue.toIntOrNull() != null,
                    onClick = { inputValue.toIntOrNull()?.let { viewModel.searchValue(it) } }
                )
            }

            Spacer(Modifier.height(12.dp))

            GlassSegmented(
                options = listOf("In-order", "Pre-order", "Post-order"),
                selectedIndex = TraversalType.entries.indexOf(selectedTraversal),
                onSelect = { index ->
                    selectedTraversal = TraversalType.entries[index]
                    viewModel.startTraversal(selectedTraversal)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (traversalResult.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = traversalResult,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = glass.accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = "Delete",
                    enabled = inputValue.toIntOrNull() != null,
                    onClick = {
                        inputValue.toIntOrNull()?.let {
                            viewModel.deleteValue(it)
                            inputValue = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Clear tree",
                    enabled = inOrder.isNotEmpty(),
                    onClick = { viewModel.clearTree() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        BstCanvas(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
}

private fun findParent(node: BstNode?, value: Int, parent: BstNode? = null): BstNode? {
    if (node == null) return null
    if (node.value == value) return parent

    return if (value < node.value) {
        findParent(node.left, value, node)
    } else {
        findParent(node.right, value, node)
    }
}

@Composable
fun BstCanvas(
    viewModel: BstViewModel,
    modifier: Modifier = Modifier
) {
    val rootNode by viewModel.root.collectAsState()
    val inOrderNodes by viewModel.inOrderNodes.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val animatedNodeColors = remember { mutableStateMapOf<Int, Color>() }
    val animatedNodeOffsets = remember { mutableStateMapOf<Int, Animatable<Offset, AnimationVector2D>>() }

    val nodeCoordinates = remember(rootNode, inOrderNodes) {
        val coords = mutableMapOf<Int, NodeCoordinates>()
        rootNode?.let { root ->
            val depths = mutableMapOf<Int, Int>()
            calculateDepths(root, 0, depths)

            val nodeWidth = 200f
            val ySpacing = 300f

            inOrderNodes.forEachIndexed { index, node ->
                // inOrderNodes arrives from an async combine, so for one frame after a
                // delete it can still hold a value this tree no longer contains. `!!` threw
                // there; skipping just drops the node for that single frame.
                val depth = depths[node.value] ?: return@forEachIndexed
                val x = (index + 1) * nodeWidth
                val y = (depth + 1) * ySpacing
                coords[node.value] = NodeCoordinates(x, y)
            }
        }
        coords
    }

    val targetColors = remember(rootNode, nodeStates) {
        val colors = mutableMapOf<Int, Color>()
        if (rootNode != null) {
            val baseColors = mutableMapOf<Int, Color>()

            fun assignBaseColors(node: BstNode, parent: BstNode?) {
                val nodeColor = if (parent == null) {
                    Color.Red // Root color
                } else if (parent == rootNode) {
                    if (node.value < parent.value) {
                        Color(0xFF4FC3F7) // Base blue for left subtree
                    } else {
                        Color(0xFF4DB6AC) // Base green for right subtree
                    }
                } else {
                    val parentColor = baseColors[parent.value]!!
                    if (node.value > parent.value) {
                        parentColor.darker()
                    } else {
                        parentColor.lighter()
                    }
                }
                baseColors[node.value] = nodeColor
                node.left?.let { assignBaseColors(it, node) }
                node.right?.let { assignBaseColors(it, node) }
            }

            assignBaseColors(rootNode!!, null)

            fun assignFinalColors(node: BstNode) {
                val state = nodeStates[node.value]
                colors[node.value] = when (state) {
                    NodeState.COMPARING -> Color(0xFFFFA500) // Orange
                    NodeState.FOUND -> Color.Green
                    NodeState.VISITED -> Color.Yellow
                    else -> baseColors[node.value] ?: Color.Gray
                }
                node.left?.let { assignFinalColors(it) }
                node.right?.let { assignFinalColors(it) }
            }
            assignFinalColors(rootNode!!)
        }
        colors
    }

    inOrderNodes.forEach { node ->
        val targetColor = targetColors[node.value] ?: Color.Gray

        val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = spring(stiffness = 50f),
            label = "NodeColorAnimation-${node.value}"
        )
        animatedNodeColors[node.value] = animatedColor
    }

    LaunchedEffect(nodeCoordinates) {
        (animatedNodeOffsets.keys - nodeCoordinates.keys).forEach {
            animatedNodeOffsets.remove(it)
        }

        nodeCoordinates.forEach { (value, targetCoords) ->
            val targetOffset = Offset(targetCoords.x, targetCoords.y)

            if (!animatedNodeOffsets.containsKey(value)) {
                val parent = findParent(rootNode, value)
                val startOffset = if (parent != null) {
                    animatedNodeOffsets[parent.value]?.value ?: nodeCoordinates[parent.value]?.let { Offset(it.x, it.y) } ?: targetOffset
                } else {
                    targetOffset
                }
                animatedNodeOffsets[value] = Animatable(startOffset, Offset.VectorConverter)
            }

            launch {
                animatedNodeOffsets[value]?.animateTo(
                    targetValue = targetOffset,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f)
                )
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
    ) {
        rootNode?.let { root ->
            if (inOrderNodes.isNotEmpty()) {
                val currentCoords = animatedNodeOffsets.mapValues { it.value.value }
                drawBstTree(root, currentCoords, animatedNodeColors)
            }
        }
    }
}

private fun calculateDepths(node: BstNode?, level: Int, depths: MutableMap<Int, Int>) {
    if (node == null) return
    depths[node.value] = level
    calculateDepths(node.left, level + 1, depths)
    calculateDepths(node.right, level + 1, depths)
}

private fun DrawScope.drawBstTree(
    node: BstNode?,
    coords: Map<Int, Offset>,
    animatedColors: Map<Int, Color>
) {
    if (node == null) return

    val nodeCenter = coords[node.value] ?: return
    val radius = 60f

    val finalColor = animatedColors[node.value] ?: Color.Gray
    val textColor = getContrastingTextColor(finalColor)

    drawCircle(color = finalColor, radius = radius, center = nodeCenter)
    drawContext.canvas.nativeCanvas.drawText(
        node.value.toString(),
        nodeCenter.x,
        nodeCenter.y + 15,
        Paint().apply {
            this.color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 40f
        }
    )

    fun drawBranch(childNode: BstNode) {
        val childCenter = coords[childNode.value] ?: return

        val vector = childCenter - nodeCenter
        val distance = vector.getDistance()
        if (distance == 0f) return

        val direction = vector / distance
        val startOffset = nodeCenter + direction * radius
        val endOffset = childCenter - direction * radius

        // Outline
        drawLine(
            Color.Black,
            start = startOffset,
            end = endOffset,
            strokeWidth = 8f
        )
        // Inner line
        val childColor = animatedColors[childNode.value] ?: Color.White
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(finalColor, childColor),
                start = startOffset,
                end = endOffset
            ),
            start = startOffset,
            end = endOffset,
            strokeWidth = 5f
        )
        drawBstTree(childNode, coords, animatedColors)
    }

    node.left?.let { drawBranch(it) }
    node.right?.let { drawBranch(it) }
}

private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}
