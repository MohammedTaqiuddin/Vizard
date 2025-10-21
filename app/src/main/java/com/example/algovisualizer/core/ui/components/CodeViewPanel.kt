package com.example.algovisualizer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun CodeViewPanel(
    code: String,
    highlightedLine: Int? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        Text(
            text = code,
            color = Color(0xFFD4D4D4),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}