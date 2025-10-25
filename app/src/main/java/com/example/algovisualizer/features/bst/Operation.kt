package com.example.algovisualizer.features.bst

data class Operation(
    val type: OperationType,
    val description: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OperationType {
    INSERT, DELETE, SEARCH, TRAVERSE, CLEAR
}
