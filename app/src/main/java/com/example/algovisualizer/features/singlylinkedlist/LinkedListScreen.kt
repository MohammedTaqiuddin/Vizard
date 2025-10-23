package com.example.algovisualizer.features.singlylinkedlist

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedListScreen(onBackClick: () -> Unit, viewModel: LinkedListViewModel = viewModel()) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var inputValue by remember { mutableStateOf("") }
    var searchValue by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var nodeToDelete by remember { mutableStateOf<SinglyLinkedListNode?>(null) }

    val isDragModeEnabled by viewModel.isDragModeEnabled.collectAsState()
    val operations by viewModel.operations.collectAsState()
    val nodes by viewModel.nodes.collectAsState()

    val tabs = listOf("Visualization", "Operations & Code")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Singly Linked List") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Info button
                        IconButton(onClick = { /* Show help */ }) {
                            Icon(Icons.Default.Info, contentDescription = "Info")
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.PlayArrow else Icons.Default.Code,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomControls(
                inputValue = inputValue,
                onInputChange = { inputValue = it },
                searchValue = searchValue,
                onSearchChange = { searchValue = it },
                onAppend = {
                    inputValue.toIntOrNull()?.let {
                        viewModel.append(it)
                        inputValue = ""
                    }
                },
                onSearch = {
                    searchValue.toIntOrNull()?.let {
                        viewModel.searchNode(it)
                    }
                },
                onClear = { viewModel.clearAll() },
                isDragModeEnabled = isDragModeEnabled,
                onToggleDragMode = { viewModel.toggleDragMode() },
                nodesCount = nodes.size
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTabIndex) {
                0 -> LinkedListCanvas(
                    viewModel = viewModel,
                    onNodeLongPress = { node ->
                        nodeToDelete = node
                        showDeleteDialog = true
                    }
                )
                1 -> OperationsPanel(
                    operations = operations,
                    onClearOperations = { viewModel.clearOperations() }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && nodeToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Node") },
            text = {
                Text("Do you want to delete node with value ${nodeToDelete?.value}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        nodeToDelete?.let { viewModel.deleteNode(it.id) }
                        showDeleteDialog = false
                        nodeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BottomControls(
    inputValue: String,
    onInputChange: (String) -> Unit,
    searchValue: String,
    onSearchChange: (String) -> Unit,
    onAppend: () -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    isDragModeEnabled: Boolean,
    onToggleDragMode: () -> Unit,
    nodesCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: Add and Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputChange,
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onAppend) {
                            Icon(Icons.Default.Add, "Append")
                        }
                    }
                )

                OutlinedTextField(
                    value = searchValue,
                    onValueChange = onSearchChange,
                    label = { Text("Search") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                )
            }

            // Second row: Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggleDragMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDragModeEnabled)
                                Color(0xFF4CAF50)
                            else
                                Color.Gray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isDragModeEnabled) "Drag ON" else "Drag OFF")
                    }

                    OutlinedButton(
                        onClick = onClear,
                        enabled = nodesCount > 0,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }

                // Node count indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$nodesCount nodes",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Help text
            Text(
                text = "💡 Long press node to delete • Use +/- buttons to zoom • 2 fingers to pan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun OperationsPanel(
    operations: List<Operation>,
    onClearOperations: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Operations History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${operations.size} operations recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onClearOperations) {
                    Icon(Icons.Default.Clear, "Clear history")
                }
            }
        }

        if (operations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No operations yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Perform operations to see code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(operations) { operation ->
                    OperationCard(operation)
                }
            }
        }
    }
}

@Composable
fun OperationCard(operation: Operation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (operation.type) {
                OperationType.APPEND -> Color(0xFFE8F5E9)
                OperationType.DELETE -> Color(0xFFFFEBEE)
                OperationType.SEARCH -> Color(0xFFE3F2FD)
                OperationType.REORDER -> Color(0xFFFFF3E0)
                OperationType.CLEAR -> Color(0xFFFCE4EC)
                OperationType.TRAVERSE -> Color(0xFFF3E5F5)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (operation.type) {
                            OperationType.APPEND -> Icons.Default.Add
                            OperationType.DELETE -> Icons.Default.Delete
                            OperationType.SEARCH -> Icons.Default.Search
                            OperationType.REORDER -> Icons.Default.SwapHoriz
                            OperationType.CLEAR -> Icons.Default.Clear
                            OperationType.TRAVERSE -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (operation.type) {
                            OperationType.APPEND -> Color(0xFF4CAF50)
                            OperationType.DELETE -> Color(0xFFF44336)
                            OperationType.SEARCH -> Color(0xFF2196F3)
                            OperationType.REORDER -> Color(0xFFFF9800)
                            OperationType.CLEAR -> Color(0xFFE91E63)
                            OperationType.TRAVERSE -> Color(0xFF9C27B0)
                        }
                    )
                    Text(
                        text = operation.type.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formatTimestamp(operation.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description
            Text(
                text = operation.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Code
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF263238)
            ) {
                Text(
                    text = operation.code,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF80CBC4)
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 1000 -> "Just now"
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        else -> "${diff / 3600000}h ago"
    }
}

@Composable
fun LinkedListCanvas(
    viewModel: LinkedListViewModel,
    onNodeLongPress: (SinglyLinkedListNode) -> Unit
) {
    val nodes by viewModel.nodes.collectAsState()
    val isDragModeEnabled by viewModel.isDragModeEnabled.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var draggedNodeId by remember { mutableStateOf<Int?>(null) }
    var dragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var longPressedNodeId by remember { mutableStateOf<Int?>(null) }

    val animatedPositions = nodes.associate { node ->
        val isDragged = node.id == draggedNodeId
        val targetPosition = if (isDragged) node.position + dragAccumulator else node.position
        node.id to animateOffsetAsState(targetValue = targetPosition, label = "node-pos-${node.id}").value
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            // Combined gesture handling for better coordination
            .pointerInput(isDragModeEnabled, nodes, scale, offset) {
                // Handle long press for delete
                detectTapGestures(
                    onLongPress = { tapOffset ->
                        val nodeWidth = 240f
                        val nodeHeight = 140f

                        // Transform screen coordinates to canvas coordinates
                        val canvasX = (tapOffset.x - offset.x) / scale
                        val canvasY = (tapOffset.y - offset.y) / scale
                        val transformedOffset = Offset(canvasX, canvasY)

                        val longPressedNode = nodes.lastOrNull { node ->
                            val nodeTopLeft = node.position - Offset(nodeWidth / 2, nodeHeight / 2)
                            val nodeRect = androidx.compose.ui.geometry.Rect(
                                nodeTopLeft.x,
                                nodeTopLeft.y,
                                nodeTopLeft.x + nodeWidth,
                                nodeTopLeft.y + nodeHeight
                            )
                            nodeRect.contains(transformedOffset)
                        }

                        longPressedNode?.let {
                            longPressedNodeId = it.id
                            onNodeLongPress(it)
                        }
                    }
                )
            }
            // Pan with 2 fingers (always enabled, no zoom)
            .pointerInput(draggedNodeId) {
                detectTransformGestures { _, pan, _, _ ->
                    if (draggedNodeId == null) {
                        offset += pan
                    }
                }
            }
            // Node dragging (when drag mode enabled)
            .pointerInput(isDragModeEnabled, nodes, scale, offset) {
                if (isDragModeEnabled) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val nodeWidth = 240f
                            val nodeHeight = 140f

                            // Transform screen coordinates to canvas coordinates
                            val canvasX = (startOffset.x - offset.x) / scale
                            val canvasY = (startOffset.y - offset.y) / scale
                            val transformedOffset = Offset(canvasX, canvasY)

                            draggedNodeId = nodes.lastOrNull { node ->
                                val nodeTopLeft = node.position - Offset(nodeWidth / 2, nodeHeight / 2)
                                val nodeRect = androidx.compose.ui.geometry.Rect(
                                    nodeTopLeft.x,
                                    nodeTopLeft.y,
                                    nodeTopLeft.x + nodeWidth,
                                    nodeTopLeft.y + nodeHeight
                                )
                                nodeRect.contains(transformedOffset)
                            }?.id

                            dragAccumulator = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            draggedNodeId?.let {
                                // Apply inverse scale to drag amount
                                dragAccumulator += dragAmount / scale
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            draggedNodeId?.let { id ->
                                val draggedNode = nodes.first { n -> n.id == id }
                                val finalPosition = draggedNode.position + dragAccumulator
                                viewModel.reorderNodes(id, finalPosition)
                            }
                            draggedNodeId = null
                            dragAccumulator = Offset.Zero
                        },
                        onDragCancel = {
                            draggedNodeId = null
                            dragAccumulator = Offset.Zero
                        }
                    )
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
        ) {
            // Paint objects
            val valuePaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = 48f
                color = Color.Black.toArgb()
                isAntiAlias = true
            }
            val addressPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = 32f
                color = Color(0xFF388E3C).toArgb()
                isAntiAlias = true
            }
            val headPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = 42f
                color = Color(0xFF1976D2).toArgb()
                isAntiAlias = true
                isFakeBoldText = true
            }
            val textBounds = Rect()

            val nodeWidth = 240f
            val nodeHeight = 140f
            val dividerOffset = 80f

            // Draw Head pointer
            if (nodes.isNotEmpty()) {
                val firstNode = nodes.first()
                val firstNodePos = animatedPositions[firstNode.id] ?: firstNode.position

                val headBoxTopLeft = Offset(120f, 250f)
                val headBoxSize = Size(120f, 80f)
                val headBoxCenter = headBoxTopLeft + Offset(headBoxSize.width / 2, headBoxSize.height / 2)

                drawContext.canvas.nativeCanvas.drawText("Head", headBoxCenter.x, headBoxTopLeft.y - 20f, headPaint)

                drawRect(
                    color = Color(0xFFE3F2FD),
                    topLeft = headBoxTopLeft,
                    size = headBoxSize
                )
                drawRect(
                    color = Color(0xFF1976D2),
                    topLeft = headBoxTopLeft,
                    size = headBoxSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(4f)
                )

                val headAddress = firstNode.address
                addressPaint.getTextBounds(headAddress, 0, headAddress.length, textBounds)
                drawContext.canvas.nativeCanvas.drawText(
                    headAddress,
                    headBoxCenter.x,
                    headBoxCenter.y - textBounds.exactCenterY(),
                    addressPaint
                )

                val headBoxBottomCenter = Offset(headBoxCenter.x, headBoxTopLeft.y + headBoxSize.height)
                val firstNodeTopCenter = firstNodePos - Offset(0f, nodeHeight / 2)

                drawLine(
                    color = Color(0xFF1976D2),
                    start = headBoxBottomCenter,
                    end = firstNodeTopCenter,
                    strokeWidth = 4f
                )

                val arrowSize = 15f
                val angle = kotlin.math.atan2(
                    firstNodeTopCenter.y - headBoxBottomCenter.y,
                    firstNodeTopCenter.x - headBoxBottomCenter.x
                )
                drawLine(
                    color = Color(0xFF1976D2),
                    start = firstNodeTopCenter,
                    end = firstNodeTopCenter - Offset(
                        arrowSize * kotlin.math.cos(angle - 0.5f),
                        arrowSize * kotlin.math.sin(angle - 0.5f)
                    ),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color(0xFF1976D2),
                    start = firstNodeTopCenter,
                    end = firstNodeTopCenter - Offset(
                        arrowSize * kotlin.math.cos(angle + 0.5f),
                        arrowSize * kotlin.math.sin(angle + 0.5f)
                    ),
                    strokeWidth = 4f
                )
            }

            // Draw nodes
            nodes.forEachIndexed { index, node ->
                val isDragged = draggedNodeId == node.id
                val animatedPos = animatedPositions[node.id] ?: node.position
                val nodeTopLeft = animatedPos - Offset(nodeWidth / 2, nodeHeight / 2)

                if (isDragged) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.3f),
                        topLeft = nodeTopLeft + Offset(8f, 8f),
                        size = Size(nodeWidth, nodeHeight)
                    )
                }

                drawRect(
                    color = if (isDragged) Color(0xFFFFF9C4) else Color.White,
                    topLeft = nodeTopLeft,
                    size = Size(nodeWidth, nodeHeight)
                )

                drawRect(
                    color = if (isDragged) Color(0xFFFFA000) else Color.Black,
                    topLeft = nodeTopLeft,
                    size = Size(nodeWidth, nodeHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isDragged) 4f else 3f)
                )

                drawLine(
                    color = if (isDragged) Color(0xFFFFA000) else Color.Black,
                    start = Offset(nodeTopLeft.x + dividerOffset, nodeTopLeft.y),
                    end = Offset(nodeTopLeft.x + dividerOffset, nodeTopLeft.y + nodeHeight),
                    strokeWidth = if (isDragged) 4f else 3f
                )

                val valueText = node.value.toString()
                valuePaint.getTextBounds(valueText, 0, valueText.length, textBounds)
                val valueX = nodeTopLeft.x + dividerOffset / 2
                val valueY = animatedPos.y - textBounds.exactCenterY()
                drawContext.canvas.nativeCanvas.drawText(valueText, valueX, valueY, valuePaint)

                val nextAddress = if (index < nodes.size - 1) nodes[index + 1].address else "null"
                addressPaint.getTextBounds(nextAddress, 0, nextAddress.length, textBounds)
                val nextAddressX = nodeTopLeft.x + dividerOffset + (nodeWidth - dividerOffset) / 2
                val nextAddressY = animatedPos.y - textBounds.exactCenterY()
                drawContext.canvas.nativeCanvas.drawText(nextAddress, nextAddressX, nextAddressY, addressPaint)

                val addressText = node.address
                addressPaint.getTextBounds(addressText, 0, addressText.length, textBounds)
                val addressY = nodeTopLeft.y + nodeHeight + 35f
                drawContext.canvas.nativeCanvas.drawText(addressText, animatedPos.x, addressY, addressPaint)

                if (index < nodes.size - 1) {
                    val nextNode = nodes[index + 1]
                    val animatedNextPos = animatedPositions[nextNode.id] ?: nextNode.position

                    val startOffset = Offset(nodeTopLeft.x + nodeWidth, animatedPos.y)
                    val endOffset = Offset(animatedNextPos.x - nodeWidth / 2, animatedNextPos.y)

                    drawLine(
                        color = Color(0xFF388E3C),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 4f
                    )

                    val arrowSize = 15f
                    val angle = kotlin.math.atan2(
                        endOffset.y - startOffset.y,
                        endOffset.x - startOffset.x
                    )
                    drawLine(
                        color = Color(0xFF388E3C),
                        start = endOffset,
                        end = endOffset - Offset(
                            arrowSize * kotlin.math.cos(angle - 0.5f),
                            arrowSize * kotlin.math.sin(angle - 0.5f)
                        ),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = Color(0xFF388E3C),
                        start = endOffset,
                        end = endOffset - Offset(
                            arrowSize * kotlin.math.cos(angle + 0.5f),
                            arrowSize * kotlin.math.sin(angle + 0.5f)
                        ),
                        strokeWidth = 4f
                    )
                }
            }

            // Empty state message
            if (nodes.isEmpty()) {
                val emptyPaint = Paint().apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 48f
                    color = Color.Gray.toArgb()
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "Add nodes to get started",
                    size.width / (2 * scale) - offset.x / scale,
                    size.height / (2 * scale) - offset.y / scale,
                    emptyPaint
                )
            }
        }

        // Zoom control buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In button
            FloatingActionButton(
                onClick = {
                    scale = (scale + 0.2f).coerceIn(0.5f, 3f)
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Reset Zoom button (1:1)
            FloatingActionButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "1:1",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Zoom Out button
            FloatingActionButton(
                onClick = {
                    scale = (scale - 0.2f).coerceIn(0.5f, 3f)
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Zoom level indicator
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}