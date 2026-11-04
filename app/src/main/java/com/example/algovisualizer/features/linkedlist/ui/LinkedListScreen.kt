package com.example.algovisualizer.features.linkedlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.core.ui.glass.GlassButton
import com.example.algovisualizer.core.ui.glass.GlassSegmented
import com.example.algovisualizer.core.ui.glass.GlassTextField
import com.example.algovisualizer.core.ui.glass.GlassVisualizerScreen
import com.example.algovisualizer.features.linkedlist.core.ListKind

class LinkedListViewModelFactory(private val kind: ListKind) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LinkedListViewModel(kind) as T
    }
}

@Composable
fun LinkedListScreen(
    initialKind: ListKind,
    onBackClick: () -> Unit
) {
    val viewModel: LinkedListViewModel = viewModel(
        key = initialKind.name,
        factory = LinkedListViewModelFactory(initialKind)
    )
    val glass = LocalGlass.current
    val state by viewModel.state.collectAsState()
    val log by viewModel.log.collectAsState()
    val error by viewModel.error.collectAsState()
    val timeline by viewModel.timeline.collectAsState()
    val frame by viewModel.frame.collectAsState()
    var input by remember { mutableStateOf("") }
    var memoryView by remember { mutableStateOf(false) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    val value = input.toIntOrNull()

    GlassVisualizerScreen(
        title = state.kind.title,
        subtitle = if (state.isEmpty) state.kind.blurb else "${state.size} nodes · ${state.kind.blurb}",
        onBack = onBackClick,
        code = pseudocodeFor(state.kind),
        highlightedLines = emptySet(),
        canvasLabel = "Memory",
        canvasIcon = Icons.Default.Timeline,
        controls = {
            GlassSegmented(
                options = ListKind.entries.map { it.label },
                selectedIndex = ListKind.entries.indexOf(state.kind),
                onSelect = { viewModel.setKind(ListKind.entries[it]) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Value",
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Head",
                    prominent = true,
                    enabled = value != null,
                    onClick = { value?.let { viewModel.insertHead(it); input = "" } }
                )
                GlassButton(
                    text = "Tail",
                    prominent = true,
                    enabled = value != null,
                    onClick = { value?.let { viewModel.insertTail(it); input = "" } }
                )
            }

            Spacer(Modifier.height(10.dp))

            GlassButton(
                text = "Search",
                enabled = value != null && !state.isEmpty,
                onClick = { value?.let { viewModel.search(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = "Del head",
                    enabled = !state.isEmpty,
                    onClick = { viewModel.deleteHead() },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Del tail",
                    enabled = !state.isEmpty,
                    onClick = { viewModel.deleteTail() },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Reverse",
                    enabled = state.size > 1,
                    onClick = { viewModel.reverse() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = "Sample",
                    onClick = { viewModel.loadSample() },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Clear",
                    enabled = !state.isEmpty,
                    onClick = { viewModel.clear() },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!timeline.isEmpty) {
                Spacer(Modifier.height(12.dp))
                PlaybackBar(
                    timeline = timeline,
                    onPlay = viewModel::play,
                    onPause = viewModel::pause,
                    onStepBack = viewModel::stepBack,
                    onStepForward = viewModel::stepForward,
                    onRestart = viewModel::restart,
                    onFinish = viewModel::finish,
                    onGoTo = viewModel::goTo,
                    onSpeed = viewModel::setSpeed,
                    onDismiss = viewModel::dismissTrace
                )
            }

            val note = error ?: log.firstOrNull()
            if (note != null && timeline.isEmpty) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (error != null) glass.nodeComparing else glass.textTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        // Chain | Memory sits above the canvas, not in the tab bar: both are views of the
        // same thing, while the tab bar switches between the picture and the source.
        GlassSegmented(
            options = listOf("Chain", "Memory"),
            selectedIndex = if (memoryView) 1 else 0,
            onSelect = { memoryView = it == 1 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .width(220.dp)
                .zIndex(1f)
        )

        if (state.isEmpty) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 56.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "The list is empty",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textSecondary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Insert a value, or load the sample",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textTertiary
                )
            }
        } else {
            LinkedListBoard(
                frame = frame,
                memoryView = memoryView,
                fits = viewModel::canPlace,
                onMove = viewModel::move,
                onBoardChanged = viewModel::setBoard,
                pan = pan,
                onPan = { pan = it },
                modifier = Modifier.fillMaxSize().padding(top = 56.dp),
                isPlaying = timeline.playing
            )
        }

        // Only meaningful once you have actually wandered off; double-tapping the board
        // does the same thing.
        if (memoryView && pan != Offset.Zero) {
            GlassButton(
                text = "Recentre",
                onClick = { pan = Offset.Zero },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .zIndex(1f)
            )
        }
    }
}

/** Static listing for now; the line-highlighted version arrives with the step engine (2b). */
private fun pseudocodeFor(kind: ListKind): String = buildString {
    appendLine("# ${kind.title}")
    appendLine()
    appendLine("insert_head(value):")
    appendLine("    newNode ← Node(value)")
    appendLine("    newNode.next ← head")
    if (kind.doubly) appendLine("    head.prev ← newNode")
    appendLine("    head ← newNode")
    if (kind.circular) appendLine("    tail.next ← head")
    appendLine()
    appendLine("insert_tail(value):")
    appendLine("    newNode ← Node(value)")
    appendLine("    tail.next ← newNode")
    if (kind.doubly) appendLine("    newNode.prev ← tail")
    appendLine("    tail ← newNode")
    appendLine(if (kind.circular) "    tail.next ← head" else "    newNode.next ← NULL")
    appendLine()
    appendLine("traverse():")
    appendLine("    curr ← head")
    appendLine(if (kind.circular) "    repeat until curr.next = head:" else "    while curr ≠ NULL:")
    appendLine("        visit(curr)")
    appendLine("        curr ← curr.next")
}
