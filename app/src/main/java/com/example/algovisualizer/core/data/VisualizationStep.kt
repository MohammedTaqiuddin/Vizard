package com.example.algovisualizer.core.data


/**
 * Represents a single step in the visualization process.
 * This is the standardized format that ALL Python scripts must return.
 */
data class VisualizationStep(
    val fullStructure: List<NodeInfo>,
    val highlightedNodeIds: Set<Int> = emptySet(),
    val comparisonNodeIds: Set<Int> = emptySet(),  // For comparisons in sorting
    val feedbackMessage: String? = null,
    val codeHighlightLine: Int? = null  // Optional: highlight a line of pseudocode
)

/**
 * Represents a node/element in any data structure.
 * Flexible enough to work for trees, lists, graphs, and arrays.
 */
data class NodeInfo(
    val id: Int,
    val value: String,
    val position: Pair<Float, Float>,  // (x, y) coordinates for rendering
    val connections: List<Int> = emptyList(),  // IDs of connected nodes
    val metadata: Map<String, Any> = emptyMap()  // Extra data (e.g., color, size)
)

/**
 * Represents the result of a Python operation.
 */
sealed class OperationResult {
    data class Success(val steps: List<VisualizationStep>) : OperationResult()
    data class Error(val message: String) : OperationResult()
}

/**
 * Feature metadata for the registry
 */
data class FeatureInfo(
    val id: String,
    val name: String,
    val category: FeatureCategory,
    val description: String,
    val iconRes: Int? = null,
    val route: String
)

enum class FeatureCategory {
    DATA_STRUCTURES,
    SORTING_ALGORITHMS,
    SEARCHING_ALGORITHMS,
    GRAPH_ALGORITHMS
}