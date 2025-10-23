package com.example.algovisualizer.features.stack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.NodeInfo
import com.example.algovisualizer.core.data.VisualizationStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack
import javax.inject.Inject

data class StackState(
    val stack: List<NodeInfo> = emptyList(),
    val visualizationSteps: List<VisualizationStep> = emptyList(),
    val stackPointer: Int = -1,
    val poppedNodeIds: Set<Int> = emptySet(),
    val peekedNodeId: Int? = null,
    val isPlaying: Boolean = false,
)

data class StackItem(val id: Int, val value: String)


@HiltViewModel
class StackViewModel @Inject constructor() : ViewModel() {

    private val _stackState = MutableStateFlow(StackState())
    val stackState: StateFlow<StackState> = _stackState.asStateFlow()

    private val stack = Stack<StackItem>()
    private val steps = mutableListOf<VisualizationStep>()
    private var nextId = 0

    fun push(value: String) {
        val intValue = value.toIntOrNull() ?: return
        val newItem = StackItem(nextId++, value)
        stack.push(newItem)
        val newStackList = stack.toList()
        val description = "Push $value onto the stack."
        steps.add(createStep(description, newStackList))
        _stackState.update {
            it.copy(
                stack = createNodeInfoList(newStackList),
                visualizationSteps = steps.toList(),
                stackPointer = newStackList.lastIndex
            )
        }
    }

    fun pop() {
        if (stack.isNotEmpty()) {
            val poppedItem = stack.peek()
            _stackState.update { it.copy(poppedNodeIds = it.poppedNodeIds + poppedItem.id) }
            viewModelScope.launch {
                delay(500) // Animation duration
                stack.pop()
                val newStackList = stack.toList()
                val description = "Pop ${poppedItem.value} from the stack."
                steps.add(createStep(description, newStackList))
                _stackState.update {
                    it.copy(
                        stack = createNodeInfoList(newStackList),
                        visualizationSteps = steps.toList(),
                        stackPointer = newStackList.lastIndex,
                        poppedNodeIds = it.poppedNodeIds - poppedItem.id
                    )
                }
            }
        }
    }


    fun peek() {
        if (stack.isNotEmpty()) {
            val peekedId = stack.peek().id
            _stackState.update { it.copy(peekedNodeId = peekedId) }
            viewModelScope.launch {
                delay(1000)
                _stackState.update { it.copy(peekedNodeId = null) }
            }
        }
    }

    fun clear() {
        stack.clear()
        steps.clear()
        nextId = 0
        _stackState.value = StackState()
    }

    private fun createStep(
        description: String,
        stack: List<StackItem>
    ): VisualizationStep {
        return VisualizationStep(
            fullStructure = createNodeInfoList(stack),
            feedbackMessage = description
        )
    }

    private fun createNodeInfoList(stack: List<StackItem>): List<NodeInfo> {
        return stack.mapIndexed { index, item ->
            NodeInfo(
                id = item.id,
                value = item.value,
                position = Pair(0f, index * 100f) // Simple vertical arrangement
            )
        }
    }
}