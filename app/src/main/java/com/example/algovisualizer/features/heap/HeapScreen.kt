package com.example.algovisualizer.features.heap

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.features.heap.components.HeapTreeVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeapScreen(
    viewModel: HeapViewModel,
    onBackClick: () -> Unit
) {
    var currentView by remember { mutableStateOf("tree") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.heapType == HeapType.MIN) "Min Heap" else "Max Heap") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    "tree" -> HeapTreeVisualizer(viewModel)
                    "code" -> CodePanel(viewModel)
                }
            }
            // Keep the controller visible in both views
            HeapController(viewModel)
        }
    }
}

@Composable
fun HeapController(viewModel: HeapViewModel) {
    var value by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                value.toIntOrNull()?.let {
                    viewModel.insertValue(it)
                    value = ""
                }
            }) {
                Text("Insert")
            }

            Button(onClick = {
                viewModel.deleteRoot()
            }) {
                Text("Delete Root")
            }

            Button(onClick = {
                viewModel.clearHeap()
            }) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun CodePanel(viewModel: HeapViewModel, modifier: Modifier = Modifier) {
    val pythonCode by viewModel.pythonCode.collectAsState()
    val highlightedLines by viewModel.highlightedCodeLines.collectAsState()

    val codeLines = remember(pythonCode) { pythonCode.lines() }
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(highlightedLines) {
        highlightedLines.firstOrNull()?.let { firstLine ->
            if (firstLine > 0) {
                val targetIndex = (firstLine - 1).coerceAtLeast(0)
                // Instantly scroll if it's far, animate otherwise
                if (lazyListState.firstVisibleItemIndex > targetIndex || lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 < targetIndex) {
                    lazyListState.scrollToItem(targetIndex)
                } else {
                    lazyListState.animateScrollToItem(index = targetIndex)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .horizontalScroll(horizontalScrollState)
    ) {
        LazyColumn(state = lazyListState) {
            itemsIndexed(codeLines) { index, line ->
                val isHighlighted = (index + 1) in highlightedLines
                Text(
                    text = if (line.isEmpty()) " " else line,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false
                )
            }
        }
    }
}