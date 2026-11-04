package com.example.algovisualizer.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.theme.LocalGlass

/**
 * Icon + accent for each feature and subcategory, keyed by registry id.
 * Colour-coding the sections gives the browse screens rhythm to scan by.
 */
fun iconForFeature(id: String): ImageVector = when (id) {
    "singly_linked_list" -> Icons.Default.Timeline
    "doubly_linked_list" -> Icons.AutoMirrored.Filled.CompareArrows
    "circular_linked_list" -> Icons.Default.Loop
    "queue" -> Icons.Default.Queue
    "bst" -> Icons.Default.AccountTree
    "avl_tree" -> Icons.Default.Balance
    "min_heap" -> Icons.Default.ArrowDownward
    "max_heap" -> Icons.Default.ArrowUpward
    "bfs" -> Icons.Default.Layers
    "dfs" -> Icons.Default.Route
    else -> Icons.Default.Storage
}

fun iconForSubCategory(id: String): ImageVector = when (id) {
    "lists" -> Icons.Default.Timeline
    "trees" -> Icons.Default.AccountTree
    "heaps" -> Icons.Default.ChangeHistory
    "graphs" -> Icons.Default.Hub
    else -> Icons.Default.Storage
}

fun iconForCategory(category: FeatureCategory): ImageVector = when (category) {
    FeatureCategory.DATA_STRUCTURES -> Icons.Default.Storage
    FeatureCategory.SORTING_ALGORITHMS -> Icons.Default.Timeline
    FeatureCategory.SEARCHING_ALGORITHMS -> Icons.Default.Route
    FeatureCategory.GRAPH_ALGORITHMS -> Icons.Default.Hub
}

@Composable
fun accentForSubCategory(id: String): Color {
    val glass = LocalGlass.current
    return when (id) {
        "lists" -> glass.orbSecondary
        "trees" -> glass.accent
        "heaps" -> glass.nodeComparing
        "graphs" -> glass.orbTertiary
        else -> glass.accent
    }
}

/** Features inherit their section's colour so a subtree reads as one family. */
@Composable
fun accentForFeature(id: String): Color = accentForSubCategory(
    when (id) {
        "singly_linked_list", "doubly_linked_list", "circular_linked_list", "queue" -> "lists"
        "bst", "avl_tree" -> "trees"
        "min_heap", "max_heap" -> "heaps"
        "bfs", "dfs" -> "graphs"
        else -> ""
    }
)
