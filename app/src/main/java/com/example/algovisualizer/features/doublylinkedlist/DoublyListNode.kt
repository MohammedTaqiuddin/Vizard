package com.example.algovisualizer.features.doublylinkedlist

import androidx.compose.ui.geometry.Offset

data class DoublyListNode(
    val value: Int,
    val id: Int,
    val address: String,
    val position: Offset = Offset.Zero,
    val next: DoublyListNode? = null,
    val prev: DoublyListNode? = null
)