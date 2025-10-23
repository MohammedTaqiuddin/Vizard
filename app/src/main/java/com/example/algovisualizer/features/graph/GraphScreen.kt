package com.example.algovisualizer.features.graph

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.algovisualizer.features.graph.components.GraphVisualizer
import kotlinx.coroutines.launch

enum class TraversalAlgorithm { BFS, DFS }
enum class InteractionMode { PAN_ZOOM, ADD_NODE, ADD_EDGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    algorithm: TraversalAlgorithm,
    onBackClick: () -> Unit,
    viewModel: GraphViewModel = viewModel()
) {
    val screenTitle = when (algorithm) {
        TraversalAlgorithm.BFS -> "Breadth-First Search"
        TraversalAlgorithm.DFS -> "Depth-First Search"
    }
    var currentView by remember { mutableStateOf("graph") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountTree, "Graph") },
                    label = { Text("Graph") },
                    selected = currentView == "graph",
                    onClick = { currentView = "graph" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Code, "Code") },
                    label = { Text("Code") },
                    selected = currentView == "code",
                    onClick = { currentView = "code" }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    "graph" -> GraphVisualizer(viewModel)
                    "code" -> CodePanel(viewModel, algorithm)
                }
            }
            GraphController(viewModel, algorithm)
        }
    }
}

@Composable
fun GraphController(
    viewModel: GraphViewModel,
    algorithm: TraversalAlgorithm
) {
    val scope = rememberCoroutineScope()
    val currentMode by viewModel.interactionMode.collectAsState()
    var startNode by remember { mutableStateOf("A") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            IconToggleButton(checked = currentMode == InteractionMode.PAN_ZOOM, onCheckedChange = { viewModel.setInteractionMode(InteractionMode.PAN_ZOOM) }) {
                Icon(Icons.Default.PanTool, "Pan and Zoom")
            }
            IconToggleButton(checked = currentMode == InteractionMode.ADD_NODE, onCheckedChange = { viewModel.setInteractionMode(InteractionMode.ADD_NODE) }) {
                Icon(Icons.Default.AddCircle, "Add Node")
            }
            IconToggleButton(checked = currentMode == InteractionMode.ADD_EDGE, onCheckedChange = { viewModel.setInteractionMode(InteractionMode.ADD_EDGE) }) {
                Icon(Icons.Default.Timeline, "Add Edge")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startNode,
                onValueChange = { startNode = it },
                label = { Text("Start Node") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                scope.launch {
                    val operation = if (algorithm == TraversalAlgorithm.BFS) "bfs" else "dfs"
                    viewModel.onOperation(operation, startNode.uppercase())
                }
            }) {
                Text("Run ${algorithm.name}")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { viewModel.onOperation("reset") } }, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
            Button(onClick = { scope.launch { viewModel.onOperation("clear") } }, modifier = Modifier.weight(1f)) {
                Text("Clear Graph")
            }
            Button(onClick = { scope.launch { viewModel.onOperation("sample") } }, modifier = Modifier.weight(1f)) {
                Text("Sample")
            }
        }
    }
}

@Composable
fun CodePanel(viewModel: GraphViewModel, algorithm: TraversalAlgorithm, modifier: Modifier = Modifier) {
    val pythonCode by viewModel.pythonCode.collectAsState()
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()

    val codeLines = remember(pythonCode) { pythonCode.lines() }
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    // This effect ensures the correct code is displayed when the screen is first shown
    LaunchedEffect(algorithm) {
        val op = if (algorithm == TraversalAlgorithm.BFS) "bfs" else "dfs"
        // A bit of a trick: we call onOperation with a non-existent node to just load the code.
        viewModel.onOperation(op, "")
    }

    LaunchedEffect(highlightedLines) {
        highlightedLines.firstOrNull()?.let { firstLine ->
            if (firstLine > 0) {
                val targetIndex = (firstLine - 1).coerceAtLeast(0)
                if (lazyListState.firstVisibleItemIndex > targetIndex || lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 < targetIndex) {
                    lazyListState.scrollToItem(targetIndex)
                } else {
                    lazyListState.animateScrollToItem(index = targetIndex)
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().padding(8.dp).horizontalScroll(horizontalScrollState)
    ) {
        LazyColumn(state = lazyListState) {
            itemsIndexed(codeLines) { index, line ->
                val isHighlighted = (index + 1) in highlightedLines
                Text(
                    text = if (line.isEmpty()) " " else line,
                    modifier = Modifier.fillMaxWidth().background(
                        if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
                    ).padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false
                )
            }
        }
    }
}