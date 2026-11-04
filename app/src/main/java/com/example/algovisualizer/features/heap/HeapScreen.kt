package com.example.algovisualizer.features.heap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.ui.glass.GlassButton
import com.example.algovisualizer.core.ui.glass.GlassTextField
import com.example.algovisualizer.core.ui.glass.GlassVisualizerScreen
import com.example.algovisualizer.features.heap.components.HeapTreeVisualizer

@Composable
fun HeapScreen(
    viewModel: HeapViewModel,
    onBackClick: () -> Unit
) {
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()
    val heap by viewModel.heap.collectAsState()
    val code by viewModel.pythonCode.collectAsState()
    var value by remember { mutableStateOf("") }

    val isMin = viewModel.heapType == HeapType.MIN

    GlassVisualizerScreen(
        title = if (isMin) "Min Heap" else "Max Heap",
        subtitle = if (heap.isEmpty()) {
            if (isMin) "Smallest value stays on top" else "Largest value stays on top"
        } else {
            "${heap.size} nodes  ·  root ${heap.first()}"
        },
        onBack = onBackClick,
        code = code,
        highlightedLines = highlightedLines,
        canvasLabel = "Tree",
        canvasIcon = Icons.Default.AccountTree,
        controls = {
            GlassTextField(
                value = value,
                onValueChange = { value = it },
                label = "Value",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Insert",
                    prominent = true,
                    enabled = value.toIntOrNull() != null,
                    onClick = {
                        value.toIntOrNull()?.let {
                            viewModel.insertValue(it)
                            value = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Delete root",
                    enabled = heap.isNotEmpty(),
                    onClick = { viewModel.deleteRoot() },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Clear",
                    enabled = heap.isNotEmpty(),
                    onClick = { viewModel.clearHeap() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        HeapTreeVisualizer(viewModel)
    }
}
