package com.example.algovisualizer.features.singlylinkedlist

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedListScreen(
    onBackClick: () -> Unit,
    viewModel: LinkedListViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf("list") }
    var inputValue by remember { mutableStateOf("") }
    // Get drag mode state from the ViewModel
    val isDragModeEnabled by viewModel.isDragModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Singly Linked List") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        inputValue.toIntOrNull()?.let {
                            viewModel.appendValue(it)
                            inputValue = ""
                        }
                    }) {
                        Text("Append")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            inputValue.toIntOrNull()?.let {
                                viewModel.prependValue(it)
                                inputValue = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Prepend")
                    }
                    Button(
                        onClick = {
                            inputValue.toIntOrNull()?.let {
                                viewModel.deleteValue(it)
                                inputValue = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            inputValue.toIntOrNull()?.let {
                                viewModel.searchValue(it)
                                inputValue = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.traverseList() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Traverse")
                    }
                    Button(
                        // Call the ViewModel to toggle the state
                        onClick = { viewModel.toggleDragMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDragModeEnabled) Color(0xFF4CAF50) else Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isDragModeEnabled) "Drag ON" else "Drag OFF")
                    }
                    Button(
                        onClick = { viewModel.clearList() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = "List") },
                        label = { Text("List") },
                        selected = currentView == "list",
                        onClick = { currentView = "list" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Code, contentDescription = "Code") },
                        label = { Text("Code") },
                        selected = currentView == "code",
                        onClick = { currentView = "code" }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentView) {
            "list" -> LinkedListCanvas(
                viewModel = viewModel,
                isDragModeEnabled = isDragModeEnabled,
                modifier = Modifier.padding(paddingValues)
            )
            "code" -> CodePanel(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun LinkedListCanvas(
    viewModel: LinkedListViewModel,
    modifier: Modifier = Modifier,
    isDragModeEnabled: Boolean
) {
    val head by viewModel.head.collectAsState()
    val nodeStates by viewModel.nodeStates.collectAsState()

    // NEW: Use rememberUpdatedState to get the latest 'head' value inside gestures
    val currentHead by rememberUpdatedState(head)

    // RE-ADD rememberUpdatedState delegates for scale/offset (Used for reading latest in lambda)
    var scale by remember { mutableStateOf(1f) }
    val currentScale by rememberUpdatedState(scale) // Latest scale value
    var offsetX by remember { mutableStateOf(0f) }
    val currentOffsetX by rememberUpdatedState(offsetX) // Latest offsetX value
    var offsetY by remember { mutableStateOf(0f) }
    val currentOffsetY by rememberUpdatedState(offsetY) // Latest offsetY value


    var draggedNodeId by remember { mutableStateOf<Int?>(null) }
    // Store the node's original position when drag starts
    var draggedNodeOriginalPosition by remember { mutableStateOf(Offset.Zero) }
    // Store the visual-only drag delta
    var nodeDragDelta by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 1. Zoom/Pan Detector (Minimal key for performance)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (!isDragModeEnabled) { // Only allow pan/zoom when drag is OFF

                            // *** START FIX: Simplified Zoom Logic ***
                            val newScale = (scale * zoom).coerceIn(0.3f, 3f)
                            scale = newScale

                            // Only update offset by pan amount
                            offsetX += pan.x
                            offsetY += pan.y
                            // *** END FIX: Simplified Zoom Logic ***
                        }
                    }
                }
                // 2. Drag Detector (Keyed only on drag mode and list content)
                .pointerInput(isDragModeEnabled, currentHead) {
                    if (isDragModeEnabled) {
                        // We are in DRAG mode
                        detectDragGestures(
                            onDragStart = { offset ->
                                val headNode = currentHead
                                if (headNode != null) {
                                    var node: ListNode? = headNode
                                    while (node != null) {
                                        // Use the latest geometric values from rememberUpdatedState delegates
                                        val nodeX = node.position.x * currentScale + currentOffsetX
                                        val nodeY = node.position.y * currentScale + currentOffsetY
                                        val nodeWidth = 250f * currentScale
                                        val nodeHeight = 120f * currentScale

                                        if (offset.x in nodeX..(nodeX + nodeWidth) &&
                                            offset.y in (nodeY - nodeHeight / 2)..(nodeY + nodeHeight / 2)
                                        ) {
                                            draggedNodeId = node.id
                                            draggedNodeOriginalPosition = node.position
                                            nodeDragDelta = Offset.Zero
                                            break
                                        }
                                        node = node.next
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val nodeId = draggedNodeId
                                if (nodeId != null) {
                                    // Use the latest scale for correcting drag amount
                                    nodeDragDelta += dragAmount / currentScale
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                val nodeId = draggedNodeId
                                if (nodeId != null) {
                                    // Calculate the final position and update the ViewModel ONCE
                                    val finalX = draggedNodeOriginalPosition.x + nodeDragDelta.x
                                    val finalY = draggedNodeOriginalPosition.y + nodeDragDelta.y
                                    viewModel.updateNodePosition(nodeId, Offset(finalX, finalY))
                                }
                                // Reset local drag state
                                draggedNodeId = null
                                nodeDragDelta = Offset.Zero
                                draggedNodeOriginalPosition = Offset.Zero
                            }
                        )
                    }
                }
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
        ) {
            val headNode = head // Drawing can use the direct state
            if (headNode != null) {
                val nodeWidth = 250f
                val nodeHeight = 120f

                // Draw arrows
                var currentNode: ListNode? = headNode
                while (currentNode != null) {
                    val nextNode = currentNode.next
                    if (nextNode != null) {

                        // Check if CURRENT node is dragged
                        val (currentVisualX, currentVisualY) = if (currentNode.id == draggedNodeId) {
                            (currentNode.position.x + nodeDragDelta.x) to (currentNode.position.y + nodeDragDelta.y)
                        } else {
                            currentNode.position.x to currentNode.position.y
                        }

                        // Check if NEXT node is dragged
                        val (nextVisualX, nextVisualY) = if (nextNode.id == draggedNodeId) {
                            (nextNode.position.x + nodeDragDelta.x) to (nextNode.position.y + nodeDragDelta.y)
                        } else {
                            nextNode.position.x to nextNode.position.y
                        }

                        // Use visual positions to draw arrows
                        val startX = currentVisualX + nodeWidth
                        val startY = currentVisualY
                        val endX = nextVisualX
                        val endY = nextVisualY

                        val path = Path().apply {
                            moveTo(startX, startY)
                            // Use a simple quadratic bezier for a gentle curve
                            val midX = (startX + endX) / 2
                            val midY = (startY + endY) / 2
                            val ctrlY = if (startY > endY) startY + 80f else startY - 80f
                            val ctrlX = (startX + endX) / 2
                            quadraticBezierTo(ctrlX, ctrlY, endX - 10f, endY) // end 10px shy for arrowhead
                        }

                        drawPath(path, Color(0xFF2196F3), style = Stroke(width = 6f))

                        // Draw arrowhead
                        val angle = kotlin.math.atan2((endY - startY).toDouble(), (endX - startX).toDouble()).toFloat()
                        // A more robust way to calculate arrowhead position near the end of the bezier
                        // For simplicity, we'll place it at the endX/endY with angle
                        val arrowOffset = 10f // match the -10f offset in bezier
                        val arrowEndX = endX - arrowOffset * kotlin.math.cos(angle)
                        val arrowEndY = endY - arrowOffset * kotlin.math.sin(angle)

                        val arrowPath = Path().apply {
                            moveTo(arrowEndX, arrowEndY)
                            lineTo(arrowEndX - 30f * kotlin.math.cos(angle - 0.5f), arrowEndY - 30f * kotlin.math.sin(angle - 0.5f))
                            lineTo(arrowEndX - 30f * kotlin.math.cos(angle + 0.5f), arrowEndY - 30f * kotlin.math.sin(angle + 0.5f))
                            close()
                        }
                        drawPath(arrowPath, Color(0xFF2196F3))
                    }
                    currentNode = nextNode
                }

                // Draw nodes
                currentNode = headNode
                while (currentNode != null) {
                    val x = currentNode.position.x
                    val y = currentNode.position.y
                    val isDragged = currentNode.id == draggedNodeId

                    // NEW: Calculate the visual position based on drag delta
                    val visualX: Float
                    val visualY: Float
                    if (isDragged) {
                        visualX = x + nodeDragDelta.x
                        visualY = y + nodeDragDelta.y
                    } else {
                        visualX = x
                        visualY = y
                    }

                    val nodeColor = when (nodeStates[currentNode.id]) {
                        NodeState.COMPARING -> Color(0xFFFFA500)
                        NodeState.FOUND -> Color.Green
                        NodeState.VISITED -> Color(0xFFFFFF00) // Changed to Yellow literal
                        else -> Color(0xFF4A90E2)
                    }

                    if (isDragged) {
                        drawRoundRect(
                            Color.Black.copy(alpha = 0.3f),
                            Offset(visualX + 5f, visualY - nodeHeight / 2 + 5f), // Use visualX/Y
                            androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
                            androidx.compose.ui.geometry.CornerRadius(15f, 15f)
                        )
                    }

                    drawRoundRect(
                        if (isDragged) nodeColor.copy(alpha = 0.8f) else nodeColor,
                        Offset(visualX, visualY - nodeHeight / 2), // Use visualX/Y
                        androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
                        androidx.compose.ui.geometry.CornerRadius(15f, 15f)
                    )

                    val dividerX = visualX + (nodeWidth * 0.65f) // Use visualX
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(dividerX, visualY - nodeHeight / 2), Offset(dividerX, visualY + nodeHeight / 2), 3f) // Use visualY

                    drawContext.canvas.nativeCanvas.drawText(
                        currentNode.value.toString(),
                        visualX + (nodeWidth * 0.32f), // Use visualX
                        visualY + 10, // Use visualY
                        Paint().apply {
                            color = Color.White.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = 50f
                            isFakeBoldText = true
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        currentNode.next?.address ?: "NULL",
                        visualX + (nodeWidth * 0.825f), // Use visualX
                        visualY + 10, // Use visualY
                        Paint().apply {
                            color = Color.White.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = 26f
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        currentNode.address,
                        visualX + nodeWidth / 2, // Use visualX
                        visualY + nodeHeight / 2 + 50, // Use visualY
                        Paint().apply {
                            color = if (isDragged) Color(0xFFE91E63).toArgb() else Color.Black.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = 32f
                            isFakeBoldText = true
                        }
                    )

                    currentNode = currentNode.next
                }

                // HEAD label
                // Head label should also follow the dragged node
                val (headVisualX, headVisualY) = if (headNode.id == draggedNodeId) {
                    (headNode.position.x + nodeDragDelta.x) to (headNode.position.y + nodeDragDelta.y)
                } else {
                    headNode.position.x to headNode.position.y
                }

                val headX = headVisualX + (nodeWidth / 2)
                val headY = headVisualY - nodeHeight / 2 - 80

                drawContext.canvas.nativeCanvas.drawText(
                    "HEAD",
                    headX,
                    headY,
                    Paint().apply {
                        color = Color(0xFFE91E63).toArgb()
                        textAlign = Paint.Align.CENTER
                        textSize = 45f
                        isFakeBoldText = true
                    }
                )

                drawLine(Color(0xFFE91E63), Offset(headX, headY + 20), Offset(headX, headVisualY - nodeHeight / 2 - 10), 5f) // Use visualY

                val headArrowPath = Path().apply {
                    moveTo(headX, headVisualY - nodeHeight / 2 - 10) // Use visualY
                    lineTo(headX - 15f, headVisualY - nodeHeight / 2 - 30) // Use visualY
                    lineTo(headX + 15f, headVisualY - nodeHeight / 2 - 30) // Use visualY
                    close()
                }
                drawPath(headArrowPath, Color(0xFFE91E63))
            }
        }

        if (head != null) {
            val tipText = if (isDragModeEnabled) {
                "💡 Drag nodes to move them!"
            } else {
                "💡 Pinch to zoom, drag to pan."
            }
            Text(
                tipText,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(12.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CodePanel(viewModel: LinkedListViewModel, modifier: Modifier = Modifier) {
    val pythonCode by viewModel.pythonCode.collectAsState()

    Box(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            pythonCode,
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}
