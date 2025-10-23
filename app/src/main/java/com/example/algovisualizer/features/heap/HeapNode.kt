package com.example.algovisualizer.features.heap

data class HeapNode(
    val value: Int,
)

data class NodeCoordinates(val x: Float, val y: Float)

enum class NodeState { NORMAL, COMPARING, FOUND, VISITED }