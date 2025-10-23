package com.example.algovisualizer.features.queue

import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.ui.BaseVisualizerViewModel
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QueueNode(val id: UUID = UUID.randomUUID(), val value: Int)

enum class NodeState {
    NORMAL,
    ENQUEUED,
    DEQUEUED
}

class QueueViewModel : BaseVisualizerViewModel() {

    private object CodeLines {
        const val ENQUEUE_DEF = 2
        const val ENQUEUE_APPEND = 3
        const val DEQUEUE_DEF = 5
        const val DEQUEUE_CHECK = 6
        const val DEQUEUE_POP = 7
    }

    private val _queue = MutableStateFlow<MutableList<QueueNode>>(mutableListOf())
    val queue: StateFlow<List<QueueNode>> = _queue.asStateFlow()

    private var animationJob: Job? = null

    private val _nodeStates = MutableStateFlow<Map<UUID, NodeState>>(emptyMap())
    val nodeStates: StateFlow<Map<UUID, NodeState>> = _nodeStates.asStateFlow()

    private val _highlightedCodeLines = MutableStateFlow<Set<Int>>(emptySet())
    val highlightedCodeLines: StateFlow<Set<Int>> = _highlightedCodeLines.asStateFlow()

    val pythonCode: String = """
# Simple Python implementation for a Queue
def enqueue(queue, value):
    queue.append(value)

def dequeue(queue):
    if not queue:
        return None
    return queue.pop(0)
    """.trimIndent()

    override suspend fun onOperation(operationName: String, vararg params: Any): OperationResult {
        return try {
            when (operationName) {
                "enqueue" -> {
                    val value = params[0].toString().toInt()
                    enqueueWithAnimation(value)
                }
                "dequeue" -> {
                    dequeueWithAnimation()
                }
                "clear" -> {
                    clearQueue()
                }
            }
            OperationResult.Success(emptyList())
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Unknown error")
        }
    }

    fun enqueueValue(value: Int) {
        viewModelScope.launch {
            onOperation("enqueue", value)
        }
    }

    fun dequeue() {
        viewModelScope.launch {
            onOperation("dequeue")
        }
    }

    fun clearQueue() {
        animationJob?.cancel()
        _queue.value = mutableListOf()
        _nodeStates.value = emptyMap()
        _highlightedCodeLines.value = emptySet()
    }

    private suspend fun enqueueWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _highlightedCodeLines.value = setOf(CodeLines.ENQUEUE_DEF, CodeLines.ENQUEUE_APPEND)

            val newNode = QueueNode(value = value)
            setNodeState(newNode.id, NodeState.ENQUEUED)

            val newList = _queue.value.toMutableList()
            newList.add(newNode)
            _queue.value = newList
            delay(700)

            setNodeState(newNode.id, NodeState.NORMAL)
            delay(500)

            _highlightedCodeLines.value = emptySet()
        }
    }

    private suspend fun dequeueWithAnimation() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            if (_queue.value.isEmpty()) {
                 _highlightedCodeLines.value = setOf(CodeLines.DEQUEUE_DEF, CodeLines.DEQUEUE_CHECK)
                 delay(700)
                 _highlightedCodeLines.value = emptySet()
                 return@launch
            }

            _highlightedCodeLines.value = setOf(CodeLines.DEQUEUE_DEF)
            delay(500)

            val frontNode = _queue.value.first()

            _highlightedCodeLines.value = setOf(CodeLines.DEQUEUE_POP)
            setNodeState(frontNode.id, NodeState.DEQUEUED)
            delay(700)

            val newList = _queue.value.toMutableList()
            newList.removeAt(0)
            _queue.value = newList
            _nodeStates.value = emptyMap()
            delay(700)

            _highlightedCodeLines.value = emptySet()
        }
    }

    private fun setNodeState(nodeId: UUID, state: NodeState) {
        val newStates = _nodeStates.value.toMutableMap()
        newStates[nodeId] = state
        _nodeStates.value = newStates
    }
}