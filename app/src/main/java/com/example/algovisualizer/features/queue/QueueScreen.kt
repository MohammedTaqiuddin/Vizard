package com.example.algovisualizer.features.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.ui.glass.GlassButton
import com.example.algovisualizer.core.ui.glass.GlassTextField
import com.example.algovisualizer.core.ui.glass.GlassVisualizerScreen
import com.example.algovisualizer.features.queue.components.QueueVisualizer

@Composable
fun QueueScreen(
    viewModel: QueueViewModel,
    onBackClick: () -> Unit
) {
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()
    val queue by viewModel.queue.collectAsState()
    var value by remember { mutableStateOf("") }

    GlassVisualizerScreen(
        title = "Queue",
        subtitle = if (queue.isEmpty()) "First in, first out" else "${queue.size} items",
        onBack = onBackClick,
        code = viewModel.pythonCode,
        highlightedLines = highlightedLines,
        canvasLabel = "Queue",
        canvasIcon = Icons.Default.Queue,
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
                    text = "Enqueue",
                    prominent = true,
                    enabled = value.toIntOrNull() != null,
                    onClick = {
                        value.toIntOrNull()?.let {
                            viewModel.enqueueValue(it)
                            value = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Dequeue",
                    enabled = queue.isNotEmpty(),
                    onClick = { viewModel.dequeue() },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Clear",
                    enabled = queue.isNotEmpty(),
                    onClick = { viewModel.clearQueue() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        QueueVisualizer(viewModel)
    }
}
