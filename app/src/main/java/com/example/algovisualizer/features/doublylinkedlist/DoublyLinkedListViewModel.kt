package com.example.algovisualizer.features.doublylinkedlist

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

class DoublyLinkedListViewModel : BaseVisualizerViewModel() {

    private val _head = MutableStateFlow<DoublyListNode?>(null)
    val head: StateFlow<DoublyListNode?> = _head.asStateFlow()

    private val _tail = MutableStateFlow<DoublyListNode?>(null)
    val tail: StateFlow<DoublyListNode?> = _tail.asStateFlow()

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
        if (!_isDragModeEnabled.value) {
            animationJob?.cancel()
            _nodeStates.value = emptyMap()
        }
        _isDragModeEnabled.value = !_isDragModeEnabled.value
    }

    fun appendValue(value: Int) {
        if (_isDragModeEnabled.value) return
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
        _tail.value = null
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
                val currentNode = current
                setNodeState(currentNode.id, NodeState.VISITED)
                delay(500)
                setNodeState(currentNode.id, NodeState.NORMAL)
                current = currentNode.next
            }

            delay(300)
            _nodeStates.value = emptyMap()
        }
    }

    fun reverseTraverseList() {
        if (_isDragModeEnabled.value) return
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _nodeStates.value = emptyMap()
            delay(200)

            var current = _tail.value
            while (current != null) {
                val currentNode = current
                setNodeState(currentNode.id, NodeState.VISITED)
                delay(500)
                setNodeState(currentNode.id, NodeState.NORMAL)
                current = currentNode.prev
            }

            delay(300)
            _nodeStates.value = emptyMap()
        }
    }

    fun updateNodePosition(nodeId: Int, newPosition: Offset) {
        val updatedHead = updateNodeInList(_head.value, nodeId, newPosition)
        _head.value = updatedHead
        _tail.value = findTail(updatedHead)
    }

    private fun findTail(node: DoublyListNode?): DoublyListNode? {
        var current = node
        while (current?.next != null) {
            current = current.next
        }
        return current
    }

    private fun updateNodeInList(node: DoublyListNode?, nodeId: Int, newPosition: Offset): DoublyListNode? {
        if (node == null) return null

        val newAddress = calculateAddressFromPosition(newPosition)

        return if (node.id == nodeId) {
            DoublyListNode(
                value = node.value,
                id = node.id,
                address = newAddress,
                position = newPosition,
                next = node.next,
                prev = node.prev
            )
        } else {
            val updatedNext = updateNodeInList(node.next, nodeId, newPosition)
            DoublyListNode(
                value = node.value,
                id = node.id,
                address = node.address,
                position = node.position,
                next = updatedNext,
                prev = node.prev
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
            val newNode = DoublyListNode(value, nodeIdCounter++, address, initialPosition, null, null)

            if (_head.value == null) {
                _head.value = newNode
                _tail.value = newNode
                setNodeState(newNode.id, NodeState.FOUND)
                delay(800)
                _nodeStates.value = emptyMap()
                return@launch
            }

            var current = _head.value
            while (current != null) {
                val currentNode = current
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
            _tail.value = newNode

            setNodeState(newNode.id, NodeState.FOUND)
            delay(800)
            _nodeStates.value = emptyMap()
        }
    }

    private fun appendToList(node: DoublyListNode?, newNode: DoublyListNode): DoublyListNode? {
        if (node == null) return newNode

        if (node.next == null) {
            val updatedNode = DoublyListNode(
                value = node.value,
                id = node.id,
                address = node.address,
                position = node.position,
                next = newNode.copy(prev = node),
                prev = node.prev
            )
            return updatedNode
        }

        return DoublyListNode(
            value = node.value,
            id = node.id,
            address = node.address,
            position = node.position,
            next = appendToList(node.next, newNode),
            prev = node.prev
        )
    }

    private fun prependWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            val initialPosition = generateInitialPosition(0)
            val address = calculateAddressFromPosition(initialPosition)
            val newNode = DoublyListNode(value, nodeIdCounter++, address, initialPosition, null, null)

            if (_head.value == null) {
                _head.value = newNode
                _tail.value = newNode
                setNodeState(newNode.id, NodeState.FOUND)
                delay(800)
                _nodeStates.value = emptyMap()
                return@launch
            }

            val shiftedList = shiftNodesRight(_head.value, 1)
            val newHead = newNode.copy(next = shiftedList?.copy(prev = newNode))
            _head.value = newHead

            setNodeState(newNode.id, NodeState.FOUND)
            delay(800)
            _nodeStates.value = emptyMap()
        }
    }

    private fun shiftNodesRight(node: DoublyListNode?, startIndex: Int): DoublyListNode? {
        if (node == null) return null

        val newPosition = generateInitialPosition(startIndex)
        val newAddress = calculateAddressFromPosition(newPosition)

        val shiftedNext = shiftNodesRight(node.next, startIndex + 1)
        return DoublyListNode(
            value = node.value,
            id = node.id,
            address = newAddress,
            position = newPosition,
            next = shiftedNext,
            prev = node.prev
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
                val newHead = headValue.next?.copy(prev = null)
                _head.value = newHead
                if (newHead == null) {
                    _tail.value = null
                }
                _nodeStates.value = emptyMap()
                return@launch
            }

            var current = _head.value
            while (current != null) {
                val currentNode = current
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
            _tail.value = findTail(_head.value)
            _nodeStates.value = emptyMap()
        }
    }

    private fun deleteFromList(node: DoublyListNode?, value: Int): DoublyListNode? {
        if (node == null) return null

        if (node.value == value) {
            return node.next?.copy(prev = node.prev)
        }

        return DoublyListNode(
            value = node.value,
            id = node.id,
            address = node.address,
            position = node.position,
            next = deleteFromList(node.next, value),
            prev = node.prev
        )
    }

    private fun searchWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var current = _head.value

            while (current != null) {
                val currentNode = current
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
            val currentNode = current
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
# Doubly Linked List Implementation

class Node:
    def __init__(self, data):
        self.data = data
        self.next = None
        self.prev = None

class DoublyLinkedList:
    def __init__(self):
        self.head = None
        self.tail = None
    
    def append(self, data):
        new_node = Node(data)
        if not self.head:
            self.head = new_node
            self.tail = new_node
            return
        
        current = self.head
        while current.next:
            current = current.next
        
        current.next = new_node
        new_node.prev = current
        self.tail = new_node
    
    def prepend(self, data):
        new_node = Node(data)
        if not self.head:
            self.head = new_node
            self.tail = new_node
            return
        
        new_node.next = self.head
        self.head.prev = new_node
        self.head = new_node
    
    def delete(self, data):
        if not self.head:
            return
        
        if self.head.data == data:
            self.head = self.head.next
            if self.head:
                self.head.prev = None
            else:
                self.tail = None
            return
        
        current = self.head
        while current:
            if current.data == data:
                if current.prev:
                    current.prev.next = current.next
                if current.next:
                    current.next.prev = current.prev
                if current == self.tail:
                    self.tail = current.prev
                return
            current = current.next
    
    def search(self, data):
        current = self.head
        while current:
            if current.data == data:
                return True
            current = current.next
        return False
    
    def traverse_forward(self):
        current = self.head
        while current:
            print(current.data, end=" <-> ")
            current = current.next
        print("None")
    
    def traverse_backward(self):
        current = self.tail
        while current:
            print(current.data, end=" <-> ")
            current = current.prev
        print("None")
        """.trimIndent()
    }
}