package com.example.algovisualizer.features.singlylinkedlist

import androidx.compose.ui.geometry.Offset

data class SinglyLinkedListNode(
    val value: Int,
    val id: Int,
    val position: Offset = Offset.Zero,
    val address: String,
)
