package com.example.algovisualizer.features.heap

import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.ui.BaseVisualizerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HeapViewModel(val heapType: HeapType) : BaseVisualizerViewModel() {

    // Simplified line numbers for code highlighting
    private object CodeLines {
        const val INSERT_DEF = 2
        const val INSERT_APPEND = 3
        const val HEAPIFY_UP_DEF = 7
        const val HEAPIFY_UP_LOOP = 8
        const val HEAPIFY_UP_COMPARE = 10
        const val HEAPIFY_UP_SWAP = 11
        const val DELETE_DEF = 15
        const val DELETE_SWAP = 17
        const val DELETE_POP = 18
        const val HEAPIFY_DOWN_DEF = 22
        const val HEAPIFY_DOWN_LOOP = 23
        const val HEAPIFY_DOWN_COMPARE = 27
        const val HEAPIFY_DOWN_SWAP = 30
    }

    private val _heap = MutableStateFlow<MutableList<Int>>(mutableListOf())
    val heap: StateFlow<List<Int>> = _heap.asStateFlow()

    private var animationJob: Job? = null

    private val _nodeStates = MutableStateFlow<Map<Int, NodeState>>(emptyMap())
    val nodeStates: StateFlow<Map<Int, NodeState>> = _nodeStates.asStateFlow()

    private val _highlightedCodeLines = MutableStateFlow<Set<Int>>(emptySet())
    val highlightedCodeLines: StateFlow<Set<Int>> = _highlightedCodeLines.asStateFlow()

    val pythonCode: StateFlow<String> = MutableStateFlow(generatePythonCode())
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    private fun generatePythonCode(): String {
        val comparison = if (heapType == HeapType.MIN) "<" else ">"
        val heapName = if (heapType == HeapType.MIN) "Min-Heap" else "Max-Heap"
        return """
# Simple Python implementation for a $heapName

def insert(heap, value):
    heap.append(value)
    _heapify_up(heap, len(heap) - 1)

def _heapify_up(heap, i):
    parent = (i - 1) // 2
    while i > 0 and heap[i] $comparison heap[parent]:
        # Swap child and parent
        heap[i], heap[parent] = heap[parent], heap[i]
        i = parent
        parent = (i - 1) // 2

def delete_root(heap):
    if not heap: return
    # Move the last element to the root
    heap[0] = heap[-1]
    heap.pop()
    if heap:
        _heapify_down(heap, 0)

def _heapify_down(heap, i):
    while True:
        left = 2 * i + 1
        right = 2 * i + 2
        smallest_or_largest = i

        # Find the smallest/largest among root, left child and right child
        if left < len(heap) and heap[left] $comparison heap[smallest_or_largest]:
            smallest_or_largest = left
        if right < len(heap) and heap[right] $comparison heap[smallest_or_largest]:
            smallest_or_largest = right

        if smallest_or_largest != i:
            heap[i], heap[smallest_or_largest] = heap[smallest_or_largest], heap[i]
            i = smallest_or_largest
        else:
            break
        """.trimIndent()
    }


    override suspend fun onOperation(operationName: String, vararg params: Any): OperationResult {
        return try {
            when (operationName) {
                "insert" -> {
                    val value = params[0] as Int
                    insertWithAnimation(value)
                }
                "delete" -> {
                    deleteRootWithAnimation()
                }
                "clear" -> {
                    clearHeap()
                }
            }
            OperationResult.Success(emptyList())
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Unknown error")
        }
    }

    fun insertValue(value: Int) {
        viewModelScope.launch {
            onOperation("insert", value)
        }
    }

    fun deleteRoot() {
        viewModelScope.launch {
            onOperation("delete")
        }
    }

    fun clearHeap() {
        animationJob?.cancel()
        _heap.value = mutableListOf()
        _nodeStates.value = emptyMap()
        _highlightedCodeLines.value = emptySet()
    }

    private suspend fun insertWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _highlightedCodeLines.value = setOf(CodeLines.INSERT_DEF, CodeLines.INSERT_APPEND)
            val newList = _heap.value.toMutableList()
            newList.add(value)
            _heap.value = newList
            setNodeState(value, NodeState.FOUND)
            delay(700)

            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_UP_DEF)
            delay(500)
            heapifyUpWithAnimation(_heap.value.size - 1)

            _nodeStates.value = emptyMap()
            _highlightedCodeLines.value = emptySet()
        }
    }

    private suspend fun deleteRootWithAnimation() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            if (_heap.value.isEmpty()) return@launch

            _highlightedCodeLines.value = setOf(CodeLines.DELETE_DEF)
            delay(500)

            val mutableHeap = _heap.value.toMutableList()
            val rootValue = mutableHeap.first()

            setNodeState(rootValue, NodeState.FOUND)
            delay(700)

            if (mutableHeap.size == 1) {
                _heap.value = mutableListOf()
                _nodeStates.value = emptyMap()
                _highlightedCodeLines.value = emptySet()
                return@launch
            }

            _highlightedCodeLines.value = setOf(CodeLines.DELETE_SWAP)
            val lastValue = mutableHeap.last()
            setNodeState(lastValue, NodeState.COMPARING)
            delay(700)

            mutableHeap[0] = lastValue
            _highlightedCodeLines.value = setOf(CodeLines.DELETE_POP)
            mutableHeap.removeAt(mutableHeap.size - 1)
            _heap.value = mutableHeap
            _nodeStates.value = mapOf(lastValue to NodeState.FOUND)
            delay(700)

            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_DOWN_DEF)
            delay(500)
            heapifyDownWithAnimation(0)

            _nodeStates.value = emptyMap()
            _highlightedCodeLines.value = emptySet()
        }
    }


    private suspend fun heapifyUpWithAnimation(startIndex: Int) {
        var currentIndex = startIndex
        val mutableHeap = _heap.value.toMutableList()

        while (currentIndex > 0) {
            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_UP_LOOP)
            delay(500)
            val parentIndex = (currentIndex - 1) / 2
            val childValue = mutableHeap[currentIndex]
            val parentValue = mutableHeap[parentIndex]

            setNodeState(childValue, NodeState.COMPARING)
            setNodeState(parentValue, NodeState.COMPARING)
            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_UP_COMPARE)
            delay(700)

            val shouldSwap = if (heapType == HeapType.MIN) childValue < parentValue else childValue > parentValue

            if (shouldSwap) {
                _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_UP_SWAP)
                delay(500)
                swap(mutableHeap, currentIndex, parentIndex)
                _heap.value = mutableHeap.toMutableList()
                setNodeState(childValue, NodeState.FOUND)
                setNodeState(parentValue, NodeState.NORMAL)
                delay(700)
                currentIndex = parentIndex
            } else {
                setNodeState(childValue, NodeState.NORMAL)
                setNodeState(parentValue, NodeState.NORMAL)
                break
            }
        }
        setNodeState(mutableHeap[currentIndex], NodeState.NORMAL)
        delay(500)
    }

    private suspend fun heapifyDownWithAnimation(startIndex: Int) {
        var currentIndex = startIndex
        val mutableHeap = _heap.value.toMutableList()

        while (true) {
            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_DOWN_LOOP)
            delay(500)
            val leftChildIndex = 2 * currentIndex + 1
            val rightChildIndex = 2 * currentIndex + 2
            var swapCandidateIndex = currentIndex

            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_DOWN_COMPARE)
            delay(500)

            if (leftChildIndex < mutableHeap.size) {
                val leftChild = mutableHeap[leftChildIndex]
                val swapCandidate = mutableHeap[swapCandidateIndex]
                val comparison = if (heapType == HeapType.MIN) leftChild < swapCandidate else leftChild > swapCandidate
                if (comparison) {
                    swapCandidateIndex = leftChildIndex
                }
            }
            if (rightChildIndex < mutableHeap.size) {
                val rightChild = mutableHeap[rightChildIndex]
                val swapCandidate = mutableHeap[swapCandidateIndex]
                val comparison = if (heapType == HeapType.MIN) rightChild < swapCandidate else rightChild > swapCandidate
                if (comparison) {
                    swapCandidateIndex = rightChildIndex
                }
            }

            if (swapCandidateIndex == currentIndex) {
                break // No swap needed
            }

            val parentValue = mutableHeap[currentIndex]
            val childValue = mutableHeap[swapCandidateIndex]
            setNodeState(parentValue, NodeState.COMPARING)
            setNodeState(childValue, NodeState.COMPARING)
            delay(700)

            _highlightedCodeLines.value = setOf(CodeLines.HEAPIFY_DOWN_SWAP)
            delay(500)
            swap(mutableHeap, currentIndex, swapCandidateIndex)
            _heap.value = mutableHeap.toMutableList()
            setNodeState(parentValue, NodeState.NORMAL)
            setNodeState(childValue, NodeState.FOUND)
            delay(700)
            currentIndex = swapCandidateIndex
        }

        if (currentIndex < mutableHeap.size) {
            setNodeState(mutableHeap[currentIndex], NodeState.NORMAL)
        }
        delay(500)
    }

    private fun setNodeState(value: Int, state: NodeState) {
        val newStates = _nodeStates.value.toMutableMap()
        newStates[value] = state
        _nodeStates.value = newStates
    }

    private fun swap(list: MutableList<Int>, i: Int, j: Int) {
        val temp = list[i]
        list[i] = list[j]
        list[j] = temp
    }
}