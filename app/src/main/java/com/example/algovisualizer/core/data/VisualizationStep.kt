package com.example.algovisualizer.core.data

data class VisualizationStep(
    val fullStructure: List<NodeInfo>,
    val highlightedNodeIds: Set<Int> = emptySet(),
    val comparisonNodeIds: Set<Int> = emptySet(),
    val feedbackMessage: String? = null,
    val codeHighlightLine: Int? = null
)