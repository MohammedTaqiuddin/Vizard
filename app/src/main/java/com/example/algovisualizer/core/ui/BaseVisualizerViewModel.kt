package com.example.algovisualizer.core.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.data.VisualizationStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel that all feature ViewModels inherit from.
 * Manages common visualization state and playback controls.
 */
abstract class BaseVisualizerViewModel : ViewModel() {

    // UI State
    protected val _uiState = MutableStateFlow(VisualizerUiState())
    val uiState: StateFlow<VisualizerUiState> = _uiState.asStateFlow()

    // Code/Tree view toggle
    private val _showCodeView = mutableStateOf(false)
    val showCodeView: State<Boolean> = _showCodeView

    // Playback state
    protected val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    protected val _currentStepIndex = mutableStateOf(0)
    val currentStepIndex: State<Int> = _currentStepIndex

    protected val _visualizationSteps = mutableStateOf<List<VisualizationStep>>(emptyList())
    val visualizationSteps: State<List<VisualizationStep>> = _visualizationSteps

    /**
     * Each feature implements this to handle its specific operations.
     * @param operationName The name of the operation (e.g., "insert", "delete", "search")
     * @param params Variable parameters needed for the operation
     */
    abstract suspend fun onOperation(operationName: String, vararg params: Any): OperationResult

    /**
     * Toggle between code view and visualization view
     */
    fun toggleCodeView() {
        _showCodeView.value = !_showCodeView.value
    }

    /**
     * Playback controls
     */
    fun play() {
        _isPlaying.value = true
        viewModelScope.launch {
            while (_isPlaying.value && _currentStepIndex.value < _visualizationSteps.value.size - 1) {
                kotlinx.coroutines.delay(1000) // 1 second per step
                nextStep()
            }
            _isPlaying.value = false
        }
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun nextStep() {
        if (_currentStepIndex.value < _visualizationSteps.value.size - 1) {
            _currentStepIndex.value++
        }
    }

    fun previousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value--
        }
    }

    fun reset() {
        _currentStepIndex.value = 0
        _isPlaying.value = false
    }

    fun goToStep(index: Int) {
        if (index in _visualizationSteps.value.indices) {
            _currentStepIndex.value = index
        }
    }
}

/**
 * Common UI state for all visualizers
 */
data class VisualizerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)