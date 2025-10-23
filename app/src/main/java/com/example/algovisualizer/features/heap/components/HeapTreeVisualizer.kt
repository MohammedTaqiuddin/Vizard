package com.example.algovisualizer.features.heap.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.features.heap.HeapViewModel
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

// Helper functions for color manipulation
private fun Color.darker(factor: Float = 0.7f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

private fun Color.lighter(factor: Float = 1.4f): Color {
    return Color(
        red = (this.red * factor).coerceAtMost(1f),
        green = (this.green * factor).coerceAtMost(1f),
        blue = (this.blue * factor).coerceAtMost(1f),
        alpha = this.alpha
    )
}

private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}


@Composable
fun HeapTreeVisualizer(viewModel: HeapViewModel) {
    val heap by viewModel.heap.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var hasCentered by remember(heap) { mutableStateOf(false) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offset += panChange
    }

    val animatedNodeColors = remember { mutableStateMapOf<Int, Color>() }
    val animatedNodeOffsets = remember { mutableStateMapOf<Int, Animatable<Offset, AnimationVector2D>>() }


    val baseColors = remember(heap) {
        val colors = mutableMapOf<Int, Color>()
        if (heap.isNotEmpty()) {
            colors[0] = Color.Red // Root color

            for (i in 1 until heap.size) {
                val parentIndex = (i - 1) / 2
                val parentColor = colors[parentIndex]!!
                val parentValue = heap[parentIndex]
                val currentValue = heap[i]

                colors[i] = if (parentIndex == 0) { // Children of root
                    if (i == 1) Color(0xFF4FC3F7) else Color(0xFF4DB6AC)
                } else {
                    if (currentValue > parentValue) parentColor.darker() else parentColor.lighter()
                }
            }
        }
        colors
    }

    val targetColors = remember(heap, nodeStates) {
        val colors = mutableMapOf<Int, Color>()
        heap.forEachIndexed { index, value ->
            val state = nodeStates[value]
            colors[index] = when (state) {
                com.example.algovisualizer.features.heap.NodeState.COMPARING -> Color.Yellow
                com.example.algovisualizer.features.heap.NodeState.FOUND -> Color.Green
                else -> baseColors[index] ?: Color.Gray
            }
        }
        colors
    }

    targetColors.forEach { (index, targetColor) ->
        val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = spring(stiffness = 50f),
            label = "NodeColorAnimation-$index"
        )
        animatedNodeColors[index] = animatedColor
    }
    val horizontalScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val nodeRadius: Float
        val levelHeight: Float
        val screenWidth: Float
        val topPadding: Float
        with(LocalDensity.current) {
            nodeRadius = 24.dp.toPx()
            levelHeight = 120.dp.toPx()
            screenWidth = constraints.maxWidth.toFloat()
            topPadding = 150.dp.toPx()
        }
        val leafHorizontalSpacing = nodeRadius * 3.5f

        val maxLevel = if (heap.isEmpty()) 0 else floor(log2(heap.size.toFloat())).toInt()
        val requiredWidth = 2f.pow(maxLevel) * leafHorizontalSpacing
        val canvasWidth = max(screenWidth, requiredWidth)

        fun getNodeTargetOffset(index: Int): Offset {
            val level = if (index == 0) 0 else floor(log2((index + 1).toFloat())).toInt()
            val y = level * levelHeight + topPadding // Add padding from the top
            val nodesInLevel = 2f.pow(level).toInt()
            val indexInLevel = (index + 1) - nodesInLevel
            val slotWidth = canvasWidth / (2f.pow(maxLevel))
            val x = (indexInLevel * 2 + 1) * (slotWidth / 2) * (2f.pow(maxLevel - level))

            return Offset(x, y)
        }

        LaunchedEffect(heap) {
            (animatedNodeOffsets.keys - heap.indices).forEach {
                animatedNodeOffsets.remove(it)
            }

            heap.forEachIndexed { index, _ ->
                val targetOffset = getNodeTargetOffset(index)
                if (!animatedNodeOffsets.containsKey(index)) {
                    val parentIndex = (index - 1) / 2
                    val startOffset = if (parentIndex >= 0) {
                        animatedNodeOffsets[parentIndex]?.value ?: getNodeTargetOffset(parentIndex)
                    } else {
                        targetOffset
                    }
                    animatedNodeOffsets[index] = Animatable(startOffset, Offset.VectorConverter)
                } else {
                    if (animatedNodeOffsets[index]?.targetValue != targetOffset) {
                        launch {
                            animatedNodeOffsets[index]?.animateTo(
                                targetValue = targetOffset,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f)
                            )
                        }
                    }
                }
            }
            animatedNodeOffsets.forEach { (index, animatable) ->
                launch {
                    animatable.animateTo(
                        targetValue = getNodeTargetOffset(index),
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f)
                    )
                }
            }

            if (!hasCentered) {
                val rootOffset = getNodeTargetOffset(0)
                val scrollTarget = (rootOffset.x - screenWidth / 2).toInt()
                horizontalScrollState.animateScrollTo(scrollTarget)
                hasCentered = true
            }
        }

        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            Canvas(modifier = Modifier
                .width(canvasWidth.dp)
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
                    hasCentered = false
                    scale = 1f
                    offset = Offset.Zero
                    return@Canvas
                }

                val currentOffsets = animatedNodeOffsets.mapValues { it.value.value }

                drawConnections(heap, currentOffsets, animatedNodeColors, nodeRadius)

                heap.forEachIndexed { index, value ->
                    currentOffsets[index]?.let { nodeOffset ->
                        val nodeColor = animatedNodeColors[index] ?: Color.Gray
                        val textColor = getContrastingTextColor(nodeColor)

                        val paint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            textSize = 18.dp.toPx()
                            color = textColor.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

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
    }
}

private fun DrawScope.drawConnections(
    heap: List<Int>,
    offsets: Map<Int, Offset>,
    colors: Map<Int, Color>,
    radius: Float
) {
    heap.forEachIndexed { i, _ ->
        val parentOffset = offsets[i] ?: return@forEachIndexed
        val parentColor = colors[i] ?: Color.Gray

        val leftChildIndex = 2 * i + 1
        val rightChildIndex = 2 * i + 2

        if (leftChildIndex < heap.size) {
            offsets[leftChildIndex]?.let { childOffset ->
                val childColor = colors[leftChildIndex] ?: Color.Gray
                drawBranch(parentOffset, childOffset, parentColor, childColor, radius)
            }
        }
        if (rightChildIndex < heap.size) {
            offsets[rightChildIndex]?.let { childOffset ->
                val childColor = colors[rightChildIndex] ?: Color.Gray
                drawBranch(parentOffset, childOffset, parentColor, childColor, radius)
            }
        }
    }
}

private fun DrawScope.drawBranch(
    start: Offset,
    end: Offset,
    startColor: Color,
    endColor: Color,
    radius: Float
) {
    val vector = end - start
    val distance = vector.getDistance()
    if (distance <= 0f) return
    val direction = vector / distance

    val startOffset = start + direction * radius
    val endOffset = end - direction * radius

    // Outline
    drawLine(
        Color.Black,
        start = startOffset,
        end = endOffset,
        strokeWidth = 8f
    )
    // Inner line
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = startOffset,
            end = endOffset
        ),
        start = startOffset,
        end = endOffset,
        strokeWidth = 5f
    )
}
