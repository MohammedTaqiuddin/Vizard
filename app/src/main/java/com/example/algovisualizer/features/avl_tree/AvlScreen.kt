package com.example.algovisualizer.features.avl

import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvlScreen(
    onBackClick: () -> Unit,
    viewModel: AvlViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf("tree") }
    var inputValue by remember { mutableStateOf("") }
    var selectedTraversal by remember { mutableStateOf(TraversalType.IN_ORDER) }

    val traversalResult by viewModel.traversalResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AVL Tree") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Tree") },
                    label = { Text("Tree") },
                    selected = currentView == "tree",
                    onClick = { currentView = "tree" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Code, contentDescription = "Code") },
                    label = { Text("Code") },
                    selected = currentView == "code",
                    onClick = { currentView = "code" }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    "tree" -> AvlCanvas(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    "code" -> CodePanel(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("Enter value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = {
                        inputValue.toIntOrNull()?.let {
                            viewModel.insertValue(it)
                            inputValue = ""
                        }
                    }) {
                        Text("Insert")
                    }
                    Button(onClick = {
                        inputValue.toIntOrNull()?.let {
                            viewModel.searchValue(it)
                        }
                    }) {
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Traversal Type:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TraversalType.values().forEach { traversalType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                selectedTraversal = traversalType
                                viewModel.startTraversal(traversalType)
                            }
                        ) {
                            RadioButton(
                                selected = selectedTraversal == traversalType,
                                onClick = {
                                    selectedTraversal = traversalType
                                    viewModel.startTraversal(traversalType)
                                }
                            )
                            Text(
                                traversalType.name.replace('_', ' ')
                                    .lowercase()
                                    .replaceFirstChar { it.titlecase() }
                            )
                        }
                    }
                }

                if (traversalResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Output: $traversalResult",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- MODIFIED SECTION: Delete and Clear buttons in a Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            inputValue.toIntOrNull()?.let {
                                viewModel.deleteValue(it)
                                inputValue = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete Node")
                    }

                    Button(
                        onClick = { viewModel.clearTree() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Tree")
                    }
                }
            }
        }
    }
}

@Composable
fun AvlCanvas(
    viewModel: AvlViewModel,
    modifier: Modifier = Modifier
) {
    val rootNode by viewModel.root.collectAsState()
    val inOrderNodes by viewModel.inOrderNodes.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // State to hold the dynamic color map for animation
    val animatedNodeColors = remember { mutableStateMapOf<Int, Color>() }

    // Calculate node coordinates once
    val nodeCoordinates = remember(rootNode, inOrderNodes) {
        val coords = mutableMapOf<Int, NodeCoordinates>()
        rootNode?.let { root ->
            val depths = mutableMapOf<Int, Int>()
            calculateDepths(root, 0, depths)

            val nodeWidth = 200f
            val ySpacing = 250f

            inOrderNodes.forEachIndexed { index, node ->
                val x = (index + 1) * nodeWidth
                val y = (depths[node.value]!! + 1) * ySpacing
                coords[node.value] = NodeCoordinates(x, y)
            }
        }
        coords
    }

    // --- Node Color Animation Logic ---
    // Update the target color for each node based on its state
    inOrderNodes.forEach { node ->
        // Get the target color based on the node's state
        val targetColor = when (nodeStates[node.value]) {
            NodeState.COMPARING -> Color(0xFFFFA500) // Orange
            NodeState.FOUND -> Color.Green
            NodeState.VISITED -> Color.Yellow
            NodeState.NORMAL, null -> when (node.value) {
                rootNode?.value -> Color.Red // Root is Red
                else -> Color(0xFF4A90E2) // Default Node Color (Blue)
            }
        }

        // Animate the color transition
        val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = spring(stiffness = 50f), // Slower, smoother animation
            label = "NodeColorAnimation-${node.value}"
        )

        // Store the animated color for use in the Canvas
        animatedNodeColors[node.value] = animatedColor
    }
    // -----------------------------------

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
                // Pass the map of *animated* colors to the drawing function
                drawAvlTree(
                    node = root,
                    coords = nodeCoordinates,
                    animatedColors = animatedNodeColors
                )
            }
        }
    }
}

private fun calculateDepths(node: AvlNode?, level: Int, depths: MutableMap<Int, Int>) {
    if (node == null) return
    depths[node.value] = level
    calculateDepths(node.left, level + 1, depths)
    calculateDepths(node.right, level + 1, depths)
}

// MODIFIED: Simplified signature to use animatedColors map
private fun DrawScope.drawAvlTree(
    node: AvlNode?,
    coords: Map<Int, NodeCoordinates>,
    animatedColors: Map<Int, Color>
) {
    if (node == null) return

    val (x, y) = coords[node.value] ?: return

    // Retrieve the animated color
    val finalColor = animatedColors[node.value] ?: Color.Gray

    // Node value and contrast color logic remains the same
    val textColor = getContrastingTextColor(finalColor)

    // --- Draw Node Circle ---
    drawCircle(color = finalColor, radius = 60f, center = Offset(x, y))

    // --- Draw Node Value ---
    drawContext.canvas.nativeCanvas.drawText(
        node.value.toString(),
        x,
        y + 15,
        Paint().apply {
            this.color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 40f
        }
    )

    // --- Draw Balance Factor ---
    val balanceFactor = node.balance
    val balanceTextColor = when {
        balanceFactor > 1 || balanceFactor < -1 -> Color.Red // Highlight imbalance
        balanceFactor != 0 -> Color.Black
        else -> Color.Gray
    }

    drawContext.canvas.nativeCanvas.drawText(
        balanceFactor.toString(),
        x + 80, // Position to the right of the node
        y + 15, // Vertically aligned with the node value
        Paint().apply {
            this.color = balanceTextColor.toArgb()
            textAlign = Paint.Align.LEFT
            textSize = 30f // Slightly smaller text for the factor
            isFakeBoldText = true // Make it stand out a bit
        }
    )

    // --- Draw Lines and Recurse ---
    node.left?.let { leftChild ->
        val (childX, childY) = coords[leftChild.value] ?: return@let
        drawLine(
            Color.Black,
            start = Offset(x, y + 60),
            end = Offset(childX, childY - 60),
            strokeWidth = 5f
        )
        // Pass the animatedColors map down recursively
        drawAvlTree(leftChild, coords, animatedColors)
    }

    node.right?.let { rightChild ->
        val (childX, childY) = coords[rightChild.value] ?: return@let
        drawLine(
            Color.Black,
            start = Offset(x, y + 60),
            end = Offset(childX, childY - 60),
            strokeWidth = 5f
        )
        // Pass the animatedColors map down recursively
        drawAvlTree(rightChild, coords, animatedColors)
    }
}

private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

// REMOVED: adjustColor and the old color logic is gone.

@Composable
fun CodePanel(
    viewModel: AvlViewModel,
    modifier: Modifier = Modifier
) {
    val pythonCode by viewModel.pythonCode.collectAsState()
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()

    // Remember the code split into lines to avoid recalculating on each recomposition
    val codeLines = remember(pythonCode) { pythonCode.lines() }

    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    // This effect will run whenever the highlighted lines change, causing the list to scroll
    LaunchedEffect(highlightedLines) {
        highlightedLines.firstOrNull()?.let { firstLine ->
            if (firstLine > 0) {
                // Scroll to the item, adjusting for 0-based index and screen layout
                val targetIndex = (firstLine - 1).coerceAtLeast(0)
                lazyListState.animateScrollToItem(index = targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp) // Reduced padding for better fit
            .horizontalScroll(horizontalScrollState) // Allow horizontal scrolling for long lines
    ) {
        LazyColumn(state = lazyListState) {
            itemsIndexed(codeLines) { index, line ->
                // Line numbers are 1-based, index is 0-based
                val isHighlighted = (index + 1) in highlightedLines

                Text(
                    text = if (line.isEmpty()) " " else line, // Render empty lines with a space
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false // Prevent lines from wrapping
                )
            }
        }
    }
}