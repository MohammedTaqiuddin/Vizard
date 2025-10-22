package com.example.algovisualizer.features.singlylinkedlist

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.ui.BaseVisualizerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data classes at package level
data class ListNode(
    val value: Int,
    val id: Int,
    val address: String,
    val position: Offset = Offset.Zero,
    val next: ListNode? = null
)

enum class NodeState { NORMAL, COMPARING, FOUND, VISITED }

class LinkedListViewModel : BaseVisualizerViewModel() {

    private val _head = MutableStateFlow<ListNode?>(null)
    val head: StateFlow<ListNode?> = _head.asStateFlow()

    private var nodeIdCounter = 0
    private var animationJob: Job? = null

    private val _nodeStates = MutableStateFlow<Map<Int, NodeState>>(emptyMap())
    val nodeStates: StateFlow<Map<Int, NodeState>> = _nodeStates.asStateFlow()

    // Drag mode state
    private val _isDragModeEnabled = MutableStateFlow(false)
    val isDragModeEnabled: StateFlow<Boolean> = _isDragModeEnabled.asStateFlow()

    val pythonCode: StateFlow<String> = MutableStateFlow(getBasePythonCode())

    override suspend fun onOperation(operationName: String, vararg params: Any): OperationResult {
        return try {
            when (operationName) {
                "append" -> {
                    val value = params[0] as Int
                    appendWithAnimation(value)
                }
                "prepend" -> {
                    val value = params[0] as Int
                    prependWithAnimation(value)
                }
                "delete" -> {
                    val value = params[0] as Int
                    deleteWithAnimation(value)
                }
                "search" -> {
                    val value = params[0] as Int
                    searchWithAnimation(value)
                }
                "clear" -> {
                    clearList()
                }
            }
            OperationResult.Success(emptyList())
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Unknown error")
        }
    }

    fun toggleDragMode() {
        // Cancel any ongoing animations when entering drag mode
        if (!_isDragModeEnabled.value) {
            animationJob?.cancel()
            _nodeStates.value = emptyMap()
        }
        _isDragModeEnabled.value = !_isDragModeEnabled.value
    }

    fun appendValue(value: Int) {
        if (_isDragModeEnabled.value) return // Disable operations in drag mode
        viewModelScope.launch {
            onOperation("append", value)
        }
    }

    fun prependValue(value: Int) {
        if (_isDragModeEnabled.value) return
        viewModelScope.launch {
            onOperation("prepend", value)
        }
    }

    fun deleteValue(value: Int) {
        if (_isDragModeEnabled.value) return
        viewModelScope.launch {
            onOperation("delete", value)
        }
    }

    fun searchValue(value: Int) {
        if (_isDragModeEnabled.value) return
        viewModelScope.launch {
            onOperation("search", value)
        }
    }

    fun clearList() {
        animationJob?.cancel()
        _head.value = null
        _nodeStates.value = emptyMap()
        nodeIdCounter = 0
        _isDragModeEnabled.value = false
    }

    fun traverseList() {
        if (_isDragModeEnabled.value) return
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _nodeStates.value = emptyMap()
            delay(200)

            var current = _head.value
            while (current != null) {
                val currentNode = current // Immutable copy for smart cast
                setNodeState(currentNode.id, NodeState.VISITED)
                delay(500)
                setNodeState(currentNode.id, NodeState.NORMAL)
                current = currentNode.next
            }

            delay(300)
            _nodeStates.value = emptyMap()
        }
    }

    fun updateNodePosition(nodeId: Int, newPosition: Offset) {
        _head.value = updateNodeInList(_head.value, nodeId, newPosition)
    }

    private fun updateNodeInList(node: ListNode?, nodeId: Int, newPosition: Offset): ListNode? {
        if (node == null) return null

        return if (node.id == nodeId) {
            val newAddress = calculateAddressFromPosition(newPosition)
            ListNode(
                value = node.value,
                id = node.id,
                address = newAddress,
                position = newPosition,
                next = node.next
            )
        } else {
            ListNode(
                value = node.value,
                id = node.id,
                address = node.address,
                position = node.position,
                next = updateNodeInList(node.next, nodeId, newPosition)
            )
        }
    }

    private fun calculateAddressFromPosition(position: Offset): String {
        val addressValue = ((position.x.toInt() / 10) + (position.y.toInt() / 10)).coerceAtLeast(0)
        return "0x${addressValue.toString(16).uppercase().padStart(4, '0')}"
    }

    private fun generateInitialPosition(index: Int): Offset {
        val x = 100f + (index * 370f)
        val y = 400f
        return Offset(x, y)
    }

    private fun appendWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            val initialPosition = generateInitialPosition(getNodeCount())
            val address = calculateAddressFromPosition(initialPosition)
            val newNode = ListNode(value, nodeIdCounter++, address, initialPosition, null)

            if (_head.value == null) {
                _head.value = newNode
                setNodeState(newNode.id, NodeState.FOUND)
                delay(800)
                _nodeStates.value = emptyMap()
                return@launch
            }

            var current = _head.value
            while (current != null) {
                val currentNode = current // Immutable copy for smart cast
                if (currentNode.next != null) {
                    setNodeState(currentNode.id, NodeState.COMPARING)
                    delay(400)
                    setNodeState(currentNode.id, NodeState.NORMAL)
                    current = currentNode.next
                } else {
                    break
                }
            }

            _head.value = appendToList(_head.value, newNode)

            setNodeState(newNode.id, NodeState.FOUND)
            delay(800)
            _nodeStates.value = emptyMap()
        }
    }

    private fun appendToList(node: ListNode?, newNode: ListNode): ListNode? {
        if (node == null) return newNode

        return ListNode(
            value = node.value,
            id = node.id,
            address = node.address,
            position = node.position,
            next = if (node.next == null) newNode else appendToList(node.next, newNode)
        )
    }

    private fun prependWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            val initialPosition = generateInitialPosition(0)
            val address = calculateAddressFromPosition(initialPosition)
            val newNode = ListNode(value, nodeIdCounter++, address, initialPosition, null)

            val shiftedList = shiftNodesRight(_head.value, 1)
            _head.value = ListNode(
                value = newNode.value,
                id = newNode.id,
                address = newNode.address,
                position = newNode.position,
                next = shiftedList
            )

            setNodeState(newNode.id, NodeState.FOUND)
            delay(800)
            _nodeStates.value = emptyMap()
        }
    }

    private fun shiftNodesRight(node: ListNode?, startIndex: Int): ListNode? {
        if (node == null) return null

        val newPosition = generateInitialPosition(startIndex)
        val newAddress = calculateAddressFromPosition(newPosition)

        return ListNode(
            value = node.value,
            id = node.id,
            address = newAddress,
            position = newPosition,
            next = shiftNodesRight(node.next, startIndex + 1)
        )
    }

    private fun deleteWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            if (_head.value == null) return@launch

            val headValue = _head.value
            if (headValue?.value == value) {
                setNodeState(headValue.id, NodeState.FOUND)
                delay(800)
                _head.value = headValue.next
                _nodeStates.value = emptyMap()
                return@launch
            }

            var current = _head.value
            while (current != null) {
                val currentNode = current // Immutable copy for smart cast
                setNodeState(currentNode.id, NodeState.COMPARING)
                delay(500)

                if (currentNode.value == value) {
                    setNodeState(currentNode.id, NodeState.FOUND)
                    delay(800)
                    break
                }

                setNodeState(currentNode.id, NodeState.NORMAL)
                current = currentNode.next
            }

            _head.value = deleteFromList(_head.value, value)
            _nodeStates.value = emptyMap()
        }
    }

    private fun deleteFromList(node: ListNode?, value: Int): ListNode? {
        if (node == null) return null

        if (node.value == value) {
            return node.next
        }

        return ListNode(
            value = node.value,
            id = node.id,
            address = node.address,
            position = node.position,
            next = deleteFromList(node.next, value)
        )
    }

    private fun searchWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var current = _head.value

            while (current != null) {
                val currentNode = current // Immutable copy for smart cast
                setNodeState(currentNode.id, NodeState.COMPARING)
                delay(500)

                if (currentNode.value == value) {
                    setNodeState(currentNode.id, NodeState.FOUND)
                    delay(1000)
                    _nodeStates.value = emptyMap()
                    return@launch
                }

                setNodeState(currentNode.id, NodeState.NORMAL)
                current = currentNode.next
            }

            delay(300)
            _nodeStates.value = emptyMap()
        }
    }

    private fun getNodeCount(): Int {
        var count = 0
        var current = _head.value
        while (current != null) {
            val currentNode = current // Immutable copy for smart cast
            count++
            current = currentNode.next
        }
        return count
    }

    private fun setNodeState(id: Int, state: NodeState) {
        val newStates = _nodeStates.value.toMutableMap()
        newStates[id] = state
        _nodeStates.value = newStates
    }

    private fun getBasePythonCode(): String {
        return """
# Linked List Implementation

class Node:
    def __init__(self, data):
        self.data = data
        self.next = None

class LinkedList:
    def __init__(self):
        self.head = None
    
    def append(self, data):
        new_node = Node(data)
        if not self.head:
            self.head = new_node
            return
        current = self.head
        while current.next:
            current = current.next
        current.next = new_node
    
    def prepend(self, data):
        new_node = Node(data)
        new_node.next = self.head
        self.head = new_node
    
    def delete(self, data):
        if not self.head:
            return
        if self.head.data == data:
            self.head = self.head.next
            return
        current = self.head
        while current.next:
            if current.next.data == data:
                current.next = current.next.next
                return
            current = current.next
    
    def search(self, data):
        current = self.head
        while current:
            if current.data == data:
                return True
            current = current.next
        return False
    
    def traverse(self):
        current = self.head
        while current:
            print(current.data, end=" -> ")
            current = current.next
        print("None")
        """.trimIndent()
    }
}