package com.example.algovisualizer.features.heap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HeapViewModelFactory(private val heapType: HeapType) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HeapViewModel(heapType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}