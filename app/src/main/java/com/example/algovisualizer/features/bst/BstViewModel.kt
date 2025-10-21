package com.example.algovisualizer.features.bst

import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.ui.BaseVisualizerViewModel

class BstViewModel : BaseVisualizerViewModel() {
    override suspend fun onOperation(operationName: String, vararg params: Any): OperationResult {
        // Will implement in Phase 2
        return OperationResult.Success(emptyList())
    }
}