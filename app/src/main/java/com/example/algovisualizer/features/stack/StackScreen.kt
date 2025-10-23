package com.example.algovisualizer.features.stack

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.algovisualizer.features.stack.components.StackVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StackScreen(
    viewModel: StackViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val stackState by viewModel.stackState.collectAsState()
    var inputValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stack") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StackVisualizer(
                stack = stackState.stack,
                peekedNodeId = stackState.peekedNodeId,
                poppedNodeIds = stackState.poppedNodeIds,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.push(inputValue)
                    inputValue = ""
                }) {
                    Text("Push")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.pop() }) {
                    Text("Pop")
                }
                Button(onClick = { viewModel.peek() }) {
                    Text("Peek")
                }
                Button(onClick = { viewModel.clear() }) {
                    Text("Clear")
                }
            }
        }
    }
}