package com.example.algovisualizer.features.doublylinkedlist

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
fun DoublyLinkedListScreen(
    onBackClick: () -> Unit,
    viewModel: DoublyLinkedListViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf("list") }
    var inputValue by remember { mutableStateOf("") }
    val isDragModeEnabled by viewModel.isDragModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doubly Linked List") },
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
                        onClick = { viewModel.reverseTraverseList() },  // New: Reverse traverse for doubly
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Rev Traverse")
                    }
                    Button(
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
            "list" -> DoublyLinkedListCanvas(
                viewModel = viewModel,
                isDragModeEnabled = isDragModeEnabled,
                modifier = Modifier.padding(paddingValues)
            )
            "code" -> DoublyCodePanel(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun DoublyLinkedListCanvas(
    viewModel: DoublyLinkedListViewModel,
    modifier: Modifier = Modifier,
    isDragModeEnabled: Boolean
) {
    val head by viewModel.head.collectAsState()
    val tail by viewModel.tail.collectAsState()  // New: Tail for doubly
    val nodeStates by viewModel.nodeStates.collectAsState()

    val currentHead by rememberUpdatedState(head)
    val currentTail by rememberUpdatedState(tail)  // New: Tail for detection

    var scale by remember { mutableStateOf(1f) }
    val currentScale by rememberUpdatedState(scale)
    var offsetX by remember { mutableStateOf(0f) }
    val currentOffsetX by rememberUpdatedState(offsetX)
    var offsetY by remember { mutableStateOf(0f) }
    val currentOffsetY by rememberUpdatedState(offsetY)

    var draggedNodeId by remember { mutableStateOf<Int?>(null) }
    var draggedNodeOriginalPosition by remember { mutableStateOf(Offset.Zero) }
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
                .pointerInput(isDragModeEnabled, currentHead, currentTail) {  // Updated keys
                    if (isDragModeEnabled) {
                        // We are in DRAG mode
                        detectDragGestures(
                            onDragStart = { offset ->
                                val headNode = currentHead
                                if (headNode != null) {
                                    var node: DoublyListNode? = headNode
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
            val headNode = head
            val tailNode = tail

            if (headNode != null && tailNode != null) {
                // ✅ FIX: Create immutable copies for smart cast
                val h = headNode
                val t = tailNode

                val nodeWidth = 250f
                val nodeHeight = 120f

                // Draw next arrows (blue)
                var current: DoublyListNode? = h
                while (current != null) {
                    val currentNode = current  // ✅ Immutable copy
                    val nextNode = currentNode.next
                    if (nextNode != null) {
                        val (currentVisualX, currentVisualY) = if (currentNode.id == draggedNodeId) {
                            (currentNode.position.x + nodeDragDelta.x) to (currentNode.position.y + nodeDragDelta.y)
                        } else {
                            currentNode.position.x to currentNode.position.y
                        }
                        val (nextVisualX, nextVisualY) = if (nextNode.id == draggedNodeId) {
                            (nextNode.position.x + nodeDragDelta.x) to (nextNode.position.y + nodeDragDelta.y)
                        } else {
                            nextNode.position.x to nextNode.position.y
                        }

                        val startX = currentVisualX + nodeWidth
                        val startY = currentVisualY
                        val endX = nextVisualX
                        val endY = nextVisualY

                        val path = Path().apply {
                            moveTo(startX, startY)
                            val midX = (startX + endX) / 2
                            val midY = (startY + endY) / 2
                            val ctrlY = if (startY > endY) startY + 80f else startY - 80f
                            quadraticBezierTo(midX, ctrlY, endX - 10f, endY)
                        }
                        drawPath(path, Color(0xFF2196F3), style = Stroke(width = 6f))  // Blue for next
                        // Arrowhead (omitted for brevity; add similar to singly)
                    }
                    current = nextNode
                }

                // Draw prev arrows (green, in reverse direction)
                current = t
                while (current != null) {
                    val currentNode = current  // ✅ Immutable copy
                    val prevNode = currentNode.prev
                    if (prevNode != null) {
                        val (currentVisualX, currentVisualY) = if (currentNode.id == draggedNodeId) {
                            (currentNode.position.x + nodeDragDelta.x) to (currentNode.position.y + nodeDragDelta.y)
                        } else {
                            currentNode.position.x to currentNode.position.y
                        }
                        val (prevVisualX, prevVisualY) = if (prevNode.id == draggedNodeId) {
                            (prevNode.position.x + nodeDragDelta.x) to (prevNode.position.y + nodeDragDelta.y)
                        } else {
                            prevNode.position.x to prevNode.position.y
                        }

                        val startX = prevVisualX + nodeWidth  // Start from prev to current
                        val startY = prevVisualY
                        val endX = currentVisualX
                        val endY = currentVisualY

                        val path = Path().apply {
                            moveTo(startX, startY)
                            val midX = (startX + endX) / 2
                            val midY = (startY + endY) / 2
                            val ctrlY = if (startY > endY) startY + 80f else startY - 80f
                            quadraticBezierTo(midX, ctrlY, endX - 10f, endY)
                        }
                        drawPath(path, Color(0xFF4CAF50), style = Stroke(width = 6f))  // Green for prev
                        // Arrowhead (add as needed)
                    }
                    current = prevNode
                }

                // Draw nodes (similar to singly)
                current = h
                while (current != null) {
                    val currentNode = current  // ✅ Immutable copy
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

                    current = currentNode.next
                }

                // HEAD and TAIL labels
                // ✅ FIX: Use safe access with immutable copies
                val (headVisualX, headVisualY) = if (h.id == draggedNodeId) {
                    (h.position.x + nodeDragDelta.x) to (h.position.y + nodeDragDelta.y)
                } else {
                    h.position.x to h.position.y
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

                drawLine(Color(0xFFE91E63), Offset(headX, headY + 20), Offset(headX, headVisualY - nodeHeight / 2 - 10), 5f)

                val headArrowPath = Path().apply {
                    moveTo(headX, headVisualY - nodeHeight / 2 - 10)
                    lineTo(headX - 15f, headVisualY - nodeHeight / 2 - 30)
                    lineTo(headX + 15f, headVisualY - nodeHeight / 2 - 30)
                    close()
                }
                drawPath(headArrowPath, Color(0xFFE91E63))

                // ✅ FIX: Use safe access with immutable copies
                val (tailVisualX, tailVisualY) = if (t.id == draggedNodeId) {
                    (t.position.x + nodeDragDelta.x) to (t.position.y + nodeDragDelta.y)
                } else {
                    t.position.x to t.position.y
                }

                val tailX = tailVisualX + (nodeWidth / 2)
                val tailY = tailVisualY + nodeHeight / 2 + 150

                drawContext.canvas.nativeCanvas.drawText(
                    "TAIL",
                    tailX,
                    tailY,
                    Paint().apply {
                        color = Color(0xFFFF9800).toArgb()
                        textAlign = Paint.Align.CENTER
                        textSize = 45f
                        isFakeBoldText = true
                    }
                )

                drawLine(Color(0xFFFF9800), Offset(tailX, tailY - 20), Offset(tailX, tailVisualY + nodeHeight / 2 + 10), 5f)

                val tailArrowPath = Path().apply {
                    moveTo(tailX, tailVisualY + nodeHeight / 2 + 10)
                    lineTo(tailX - 15f, tailVisualY + nodeHeight / 2 + 30)
                    lineTo(tailX + 15f, tailVisualY + nodeHeight / 2 + 30)
                    close()
                }
                drawPath(tailArrowPath, Color(0xFFFF9800))
            }
        }

        if (head != null) {
            val tipText = if (isDragModeEnabled) {
                "💡 Drag nodes to move them! (Next: Blue, Prev: Green)"
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
fun DoublyCodePanel(viewModel: DoublyLinkedListViewModel, modifier: Modifier = Modifier) {
    val pythonCode by viewModel.pythonCode.collectAsState()

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
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