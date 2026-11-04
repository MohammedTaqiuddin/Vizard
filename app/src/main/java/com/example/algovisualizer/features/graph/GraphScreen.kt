package com.example.algovisualizer.features.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.core.ui.glass.GlassButton
import com.example.algovisualizer.core.ui.glass.GlassIconButton
import com.example.algovisualizer.core.ui.glass.GlassTextField
import com.example.algovisualizer.core.ui.glass.GlassVisualizerScreen
import com.example.algovisualizer.features.graph.components.GraphVisualizer
import kotlinx.coroutines.launch

enum class TraversalAlgorithm { BFS, DFS }
enum class InteractionMode { PAN_ZOOM, ADD_NODE, ADD_EDGE }

@Composable
fun GraphScreen(
    algorithm: TraversalAlgorithm,
    onBackClick: () -> Unit,
    viewModel: GraphViewModel = viewModel()
) {
    val glass = LocalGlass.current
    val scope = rememberCoroutineScope()
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()
    val currentMode by viewModel.interactionMode.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    var startNode by remember { mutableStateOf("A") }

    // The listing is a pure function of the algorithm, so the screen owns it. (The old version
    // fired a fake traversal just to populate it, which cancelled any running animation.)
    val code = remember(algorithm) {
        if (algorithm == TraversalAlgorithm.BFS) viewModel.generateBfsCode()
        else viewModel.generateDfsCode()
    }

    GlassVisualizerScreen(
        title = if (algorithm == TraversalAlgorithm.BFS) "Breadth-First Search" else "Depth-First Search",
        subtitle = if (nodes.isEmpty()) "Add nodes, or load the sample" else "${nodes.size} nodes",
        onBack = onBackClick,
        code = code,
        highlightedLines = highlightedLines,
        canvasLabel = "Graph",
        canvasIcon = Icons.Default.Hub,
        controls = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textTertiary,
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(
                    icon = Icons.Default.PanTool,
                    contentDescription = "Pan and zoom",
                    active = currentMode == InteractionMode.PAN_ZOOM,
                    onClick = { viewModel.setInteractionMode(InteractionMode.PAN_ZOOM) }
                )
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = Icons.Default.AddCircle,
                    contentDescription = "Add node",
                    active = currentMode == InteractionMode.ADD_NODE,
                    onClick = { viewModel.setInteractionMode(InteractionMode.ADD_NODE) }
                )
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = Icons.Default.Timeline,
                    contentDescription = "Add edge",
                    active = currentMode == InteractionMode.ADD_EDGE,
                    onClick = { viewModel.setInteractionMode(InteractionMode.ADD_EDGE) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextField(
                    value = startNode,
                    onValueChange = { startNode = it },
                    label = "Start",
                    numeric = false,
                    modifier = Modifier.width(110.dp)
                )
                GlassButton(
                    text = "Run ${algorithm.name}",
                    prominent = true,
                    enabled = nodes.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            val op = if (algorithm == TraversalAlgorithm.BFS) "bfs" else "dfs"
                            viewModel.onOperation(op, startNode.uppercase())
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Reset",
                    onClick = { scope.launch { viewModel.onOperation("reset") } },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Clear",
                    onClick = { scope.launch { viewModel.onOperation("clear") } },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Sample",
                    onClick = { scope.launch { viewModel.onOperation("sample") } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        GraphVisualizer(viewModel)
    }
}
