package com.example.algovisualizer.features.graph

import androidx.compose.ui.geometry.Offset

// Using a String for ID to be more flexible than Int
data class GraphNode(
    val id: String,
    val label: String,
    var offset: Offset = Offset.Zero // Position on the canvas
)

data class GraphEdge(
    val from: String, // ID of the source node
    val to: String    // ID of the destination node
)

// States for visualizing traversal algorithms
enum class GraphNodeState {
    IDLE,      // Default state
    CURRENT,   // The node currently being processed
    VISITED,   // Node has been visited
    IN_QUEUE,  // Node is in the queue/stack to be processed
    PATH,      // Node is part of the final path
    SELECTED   // Node is selected as the first node for an edge
}