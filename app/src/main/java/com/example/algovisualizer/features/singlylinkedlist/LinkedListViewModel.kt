package com.example.algovisualizer.features.singlylinkedlist

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Data classes


data class Operation(
    val type: OperationType,
    val description: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OperationType {
    APPEND, DELETE, REORDER, SEARCH, TRAVERSE, CLEAR
}

class LinkedListViewModel : ViewModel() {
    private val _nodes = MutableStateFlow<List<SinglyLinkedListNode>>(emptyList())
    val nodes: StateFlow<List<SinglyLinkedListNode>> = _nodes.asStateFlow()

    private val _isDragModeEnabled = MutableStateFlow(false)
    val isDragModeEnabled: StateFlow<Boolean> = _isDragModeEnabled.asStateFlow()

    private val _operations = MutableStateFlow<List<Operation>>(emptyList())
    val operations: StateFlow<List<Operation>> = _operations.asStateFlow()

    private var nextId = 0
    private val baseX = 500f
    private val baseY = 500f
    private val horizontalSpacing = 350f

    init {
        addOperation(
            type = OperationType.TRAVERSE,
            description = "Linked List initialized",
            code = """
                class Node {
                    int data;
                    Node next;
                }
                
                Node head = null;
            """.trimIndent()
        )
    }

    fun append(value: Int) {
        val newId = nextId++
        val position = Offset(
            baseX + (_nodes.value.size * horizontalSpacing),
            baseY
        )
        val address = String.format("0x%04X", (0x1000 + newId * 0x10))

        val newNode = SinglyLinkedListNode(
            id = newId,
            value = value,
            position = position,
            address = address
        )

        _nodes.value = _nodes.value + newNode

        addOperation(
            type = OperationType.APPEND,
            description = "Appended value $value to the end of the list",
            code = """
                // Create new node
                Node newNode = new Node();
                newNode.data = $value;
                newNode.next = null;
                
                // If list is empty
                if (head == null) {
                    head = newNode;
                    return;
                }
                
                // Traverse to end
                Node current = head;
                while (current.next != null) {
                    current = current.next;
                }
                
                // Link new node
                current.next = newNode;
                
                // Time Complexity: O(n)
                // Space Complexity: O(1)
            """.trimIndent()
        )
    }

    fun deleteNode(nodeId: Int) {
        val nodeToDelete = _nodes.value.find { it.id == nodeId } ?: return
        val index = _nodes.value.indexOf(nodeToDelete)

        _nodes.value = _nodes.value.filter { it.id != nodeId }

        val description = if (index == 0) {
            "Deleted head node (value: ${nodeToDelete.value})"
        } else {
            "Deleted node at position $index (value: ${nodeToDelete.value})"
        }

        addOperation(
            type = OperationType.DELETE,
            description = description,
            code = if (index == 0) {
                """
                // Delete head node
                if (head != null) {
                    Node temp = head;
                    head = head.next;
                    temp = null; // Free memory
                }
                
                // Time Complexity: O(1)
                // Space Complexity: O(1)
                """.trimIndent()
            } else {
                """
                // Delete node at position $index
                Node current = head;
                int pos = 0;
                
                // Traverse to node before target
                while (pos < ${index - 1} && current != null) {
                    current = current.next;
                    pos++;
                }
                
                // Update link to skip deleted node
                if (current != null && current.next != null) {
                    Node temp = current.next;
                    current.next = current.next.next;
                    temp = null; // Free memory
                }
                
                // Time Complexity: O(n)
                // Space Complexity: O(1)
                """.trimIndent()
            }
        )
    }

    fun clearAll() {
        _nodes.value = emptyList()
        nextId = 0

        addOperation(
            type = OperationType.CLEAR,
            description = "Cleared entire linked list",
            code = """
                // Delete all nodes
                while (head != null) {
                    Node temp = head;
                    head = head.next;
                    temp = null;
                }
                
                // Time Complexity: O(n)
                // Space Complexity: O(1)
            """.trimIndent()
        )
    }

    fun reorderNodes(draggedNodeId: Int, newPosition: Offset) {
        val currentNodes = _nodes.value.toMutableList()
        val draggedIndex = currentNodes.indexOfFirst { it.id == draggedNodeId }

        if (draggedIndex == -1) return

        val draggedNode = currentNodes[draggedIndex]
        val updatedNode = draggedNode.copy(position = newPosition)
        currentNodes[draggedIndex] = updatedNode

        currentNodes.sortBy { it.position.x }

        _nodes.value = currentNodes

        addOperation(
            type = OperationType.REORDER,
            description = "Reordered nodes - Node with value ${draggedNode.value} moved",
            code = """
                // Reordering requires modifying links
                // This is a visual representation
                // In actual implementation:
                
                // 1. Remove node from current position
                // 2. Insert at new position
                // 3. Update all next pointers
                
                // Example: Insert node at position
                Node current = head;
                for (int i = 0; i < position-1; i++) {
                    current = current.next;
                }
                
                Node newNode = ...; // node to insert
                newNode.next = current.next;
                current.next = newNode;
                
                // Time Complexity: O(n)
            """.trimIndent()
        )
    }

    fun toggleDragMode() {
        _isDragModeEnabled.value = !_isDragModeEnabled.value
    }

    fun searchNode(value: Int): Int {
        val index = _nodes.value.indexOfFirst { it.value == value }

        addOperation(
            type = OperationType.SEARCH,
            description = if (index != -1) {
                "Found value $value at position $index"
            } else {
                "Value $value not found in list"
            },
            code = """
                // Linear search in linked list
                Node current = head;
                int position = 0;
                
                while (current != null) {
                    if (current.data == $value) {
                        return position;
                    }
                    current = current.next;
                    position++;
                }
                
                return -1; // Not found
                
                // Time Complexity: O(n)
                // Space Complexity: O(1)
            """.trimIndent()
        )

        return index
    }

    fun clearOperations() {
        _operations.value = emptyList()
    }

    private fun addOperation(type: OperationType, description: String, code: String) {
        val operation = Operation(type, description, code)
        _operations.value = listOf(operation) + _operations.value.take(9)
    }
}