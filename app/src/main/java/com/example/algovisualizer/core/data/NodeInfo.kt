package com.example.algovisualizer.core.data

data class NodeInfo(
    val id: Int,
    val value: String,
    val position: Pair<Float, Float>,
    val connections: List<Int> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

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

sealed class OperationResult {
    data class Success(val steps: List<VisualizationStep>) : OperationResult()
    data class Error(val message: String) : OperationResult()
}