package com.example.algovisualizer.features.bst

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable // <-- ADDED THIS IMPORT
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun BstScreen(
    onBackClick: () -> Unit,
    viewModel: BstViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf("tree") }
    var inputValue by remember { mutableStateOf("") }
    var selectedTraversal by remember { mutableStateOf(TraversalType.IN_ORDER) }

    val traversalResult by viewModel.traversalResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Binary Search Tree") },
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
                    "tree" -> BstCanvas(
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
                val nodeCoordinates = mutableMapOf<Int, NodeCoordinates>()
                val depths = mutableMapOf<Int, Int>()
                calculateDepths(root, 0, depths)

                val nodeWidth = 200f
                val ySpacing = 250f

                inOrderNodes.forEachIndexed { index, node ->
                    val x = (index + 1) * nodeWidth
                    val y = (depths[node.value]!! + 1) * ySpacing
                    nodeCoordinates[node.value] = NodeCoordinates(x, y)
                }

                drawBstTree(root, nodeCoordinates, nodeStates, Color.Red, isRoot = true)
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
    coords: Map<Int, NodeCoordinates>,
    nodeStates: Map<Int, NodeState>,
    color: Color,
    isRoot: Boolean = false
) {
    if (node == null) return

    val (x, y) = coords[node.value] ?: return

    val animationColor = when (nodeStates[node.value]) {
        NodeState.COMPARING -> Color(0xFFFFA500) // Orange
        NodeState.FOUND -> Color.Green
        NodeState.VISITED -> Color.Yellow
        else -> null
    }

    val finalColor = animationColor ?: color
    val textColor = if (isRoot) Color.White else getContrastingTextColor(finalColor)

    drawCircle(color = finalColor, radius = 60f, center = Offset(x, y))
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

    val baseLeftColor = if (isRoot) Color(0xFF4A90E2) else color
    val baseRightColor = if (isRoot) Color(0xFF50E3C2) else color

    node.left?.let { leftChild ->
        val (childX, childY) = coords[leftChild.value] ?: return@let
        val childColor = adjustColor(baseLeftColor, 1.2f)
        drawLine(
            Color.Black,
            start = Offset(x, y + 60),
            end = Offset(childX, childY - 60),
            strokeWidth = 5f
        )
        drawBstTree(leftChild, coords, nodeStates, childColor)
    }

    node.right?.let { rightChild ->
        val (childX, childY) = coords[rightChild.value] ?: return@let
        val childColor = adjustColor(baseRightColor, 0.8f)
        drawLine(
            Color.Black,
            start = Offset(x, y + 60),
            end = Offset(childX, childY - 60),
            strokeWidth = 5f
        )
        drawBstTree(rightChild, coords, nodeStates, childColor)
    }
}

private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

private fun adjustColor(color: Color, factor: Float): Color {
    val red = (color.red * factor).coerceIn(0f, 1f)
    val green = (color.green * factor).coerceIn(0f, 1f)
    val blue = (color.blue * factor).coerceIn(0f, 1f)
    return Color(red, green, blue, 1f)
}

@Composable
fun CodePanel(
    viewModel: BstViewModel,
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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable // <-- ADDED THIS IMPORT
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun BstScreen(
    onBackClick: () -> Unit,
    viewModel: BstViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf("tree") }
    var inputValue by remember { mutableStateOf("") }
    var selectedTraversal by remember { mutableStateOf(TraversalType.IN_ORDER) }

    val traversalResult by viewModel.traversalResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Binary Search Tree") },
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
                    "tree" -> BstCanvas(
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
                val nodeCoordinates = mutableMapOf<Int, NodeCoordinates>()
                val depths = mutableMapOf<Int, Int>()
                calculateDepths(root, 0, depths)

                val nodeWidth = 200f
                val ySpacing = 250f

                inOrderNodes.forEachIndexed { index, node ->
                    val x = (index + 1) * nodeWidth
                    val y = (depths[node.value]!! + 1) * ySpacing
                    nodeCoordinates[node.value] = NodeCoordinates(x, y)
                }

                drawBstTree(root, nodeCoordinates, nodeStates, Color.Red, isRoot = true)
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
    coords: Map<Int, NodeCoordinates>,
    nodeStates: Map<Int, NodeState>,
    color: Color,
    isRoot: Boolean = false
) {
    if (node == null) return

    val (x, y) = coords[node.value] ?: return

    val animationColor = when (nodeStates[node.value]) {
        NodeState.COMPARING -> Color(0xFFFFA500) // Orange
        NodeState.FOUND -> Color.Green
        NodeState.VISITED -> Color.Yellow
        else -> null
    }

    val finalColor = animationColor ?: color
    val textColor = if (isRoot) Color.White else getContrastingTextColor(finalColor)

    drawCircle(color = finalColor, radius = 60f, center = Offset(x, y))
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

    val baseLeftColor = if (isRoot) Color(0xFF4A90E2) else color
    val baseRightColor = if (isRoot) Color(0xFF50E3C2) else color

    node.left?.let { leftChild ->
        val (childX, childY) = coords[leftChild.value] ?: return@let
        val childColor = adjustColor(baseLeftColor, 1.2f)
        drawLine(
            Color.Black,
            start = Offset(x, y + 60),
            end = Offset(childX, childY - 60),
            strokeWidth = 5f
        )
        drawBstTree(leftChild, coords, nodeStates, childColor)
    }

    node.right?.let { rightChild ->
        val (childX, childY) = coords[rightChild.value] ?: return@let
        val childColor = adjustColor(baseRightColor, 0.8f)
        drawLine(
            Color.Black,
            start = Offset(x, y + 60),
            end = Offset(childX, childY - 60),
            strokeWidth = 5f
        )
        drawBstTree(rightChild, coords, nodeStates, childColor)
    }
}

private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

private fun adjustColor(color: Color, factor: Float): Color {
    val red = (color.red * factor).coerceIn(0f, 1f)
    val green = (color.green * factor).coerceIn(0f, 1f)
    val blue = (color.blue * factor).coerceIn(0f, 1f)
    return Color(red, green, blue, 1f)
}

@Composable
fun CodePanel(
    viewModel: BstViewModel,
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