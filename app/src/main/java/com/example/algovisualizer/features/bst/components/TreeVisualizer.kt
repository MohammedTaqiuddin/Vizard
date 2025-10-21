package com.example.algovisualizer.features.bst.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun TreeVisualizer() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Placeholder - will implement drawing logic in Phase 2
        drawCircle(
            color = Color.Gray,
            radius = 50f,
            center = center
        )
    }
}