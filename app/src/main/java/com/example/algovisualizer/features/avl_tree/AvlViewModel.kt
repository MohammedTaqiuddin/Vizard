package com.example.algovisualizer.features.avl

import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.ui.BaseVisualizerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

data class AvlNode(
    val value: Int,
    var left: AvlNode? = null,
    var right: AvlNode? = null,
    var height: Int = 1
)

data class NodeCoordinates(val x: Float, val y: Float)

enum class NodeState { NORMAL, COMPARING, FOUND, VISITED }
enum class TraversalType { IN_ORDER, PRE_ORDER, POST_ORDER }

class AvlViewModel : BaseVisualizerViewModel() {

    // Object to map algorithm steps to Python code line numbers
    private object CodeLines {
        const val INSERT_DEF = 7
        const val INSERT_ROOT_IS_NULL = 8
        const val INSERT_RETURN_NEW_NODE = 9
        const val INSERT_CHECK_LESS = 10
        const val INSERT_GO_RIGHT = 11
        const val INSERT_ELSE = 12
        const val INSERT_GO_LEFT = 13
        const val INSERT_UPDATE_HEIGHT = 14
        const val INSERT_CHECK_BALANCE = 15
        const val INSERT_RETURN_ROOT = 16
        const val LL_ROTATE = 17
        const val RR_ROTATE = 18
        const val LR_ROTATE = 19
        const val RL_ROTATE = 20
    }

    private val _root = MutableStateFlow<AvlNode?>(null)
    val root: StateFlow<AvlNode?> = _root.asStateFlow()

    private var animationJob: Job? = null

    private val _nodeStates = MutableStateFlow<Map<Int, NodeState>>(emptyMap())
    val nodeStates: StateFlow<Map<Int, NodeState>> = _nodeStates.asStateFlow()

    private val _currentTraversal = MutableStateFlow(TraversalType.IN_ORDER)
    val currentTraversal: StateFlow<TraversalType> = _currentTraversal.asStateFlow()

    private val _traversalResult = MutableStateFlow("")
    val traversalResult: StateFlow<String> = _traversalResult.asStateFlow()

    // NEW: StateFlow to hold the currently highlighted code lines
    private val _highlightedCodeLines = MutableStateFlow<Set<Int>>(emptySet())
    val highlightedCodeLines: StateFlow<Set<Int>> = _highlightedCodeLines.asStateFlow()

    val inOrderNodes: StateFlow<List<AvlNode>> = root.combine(_nodeStates) { _, _ ->
        inOrderTraversal()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Code generation
    val pythonCode: StateFlow<String> = root.combine(_currentTraversal) { treeRoot, traversal ->
        generatePythonCode(treeRoot, traversal)
    }.stateIn(viewModelScope, SharingStarted.Lazily, getBasePythonCode())

    override suspend fun onOperation(operationName: String, vararg params: Any): OperationResult {
        return try {
            when (operationName) {
                "insert" -> {
                    val value = params[0] as Int
                    insertWithAnimation(value)
                }
                "search" -> {
                    val value = params[0] as Int
                    searchWithAnimation(value)
                }
                "delete" -> {
                    val value = params[0] as Int
                    deleteWithAnimation(value)
                }
                "clear" -> {
                    clearTree()
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

    fun searchValue(value: Int) {
        viewModelScope.launch {
            onOperation("search", value)
        }
    }

    fun deleteValue(value: Int) {
        viewModelScope.launch {
            onOperation("delete", value)
        }
    }

    fun clearTree() {
        animationJob?.cancel()
        _root.value = null
        _nodeStates.value = emptyMap()
        _traversalResult.value = ""
        _highlightedCodeLines.value = emptySet() // NEW: Clear highlights
    }

    fun startTraversal(type: TraversalType) {
        _currentTraversal.value = type
        val nodes = when (type) {
            TraversalType.IN_ORDER -> inOrderTraversal()
            TraversalType.PRE_ORDER -> preOrderTraversal()
            TraversalType.POST_ORDER -> postOrderTraversal()
        }

        _traversalResult.value = nodes.joinToString("  →  ") { it.value.toString() }

        animateTraversal(nodes)
    }

    // MODIFIED: insertWithAnimation now updates highlighted code lines
    private fun insertWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _highlightedCodeLines.value = setOf(CodeLines.INSERT_DEF)
            delay(500)

            if (_root.value == null) {
                _highlightedCodeLines.value = setOf(CodeLines.INSERT_ROOT_IS_NULL)
                delay(700)
                _root.value = AvlNode(value)
                _nodeStates.value = mapOf(value to NodeState.FOUND)
                _highlightedCodeLines.value = setOf(CodeLines.INSERT_RETURN_NEW_NODE)
                delay(1000)
                _nodeStates.value = emptyMap()
                _highlightedCodeLines.value = emptySet()
                return@launch
            }

            var current = _root.value
            while (current != null && current.value != value) {
                setNodeState(current.value, NodeState.COMPARING)
                _highlightedCodeLines.value = setOf(CodeLines.INSERT_CHECK_LESS)
                delay(700)

                // We need to keep the node state visually "comparing" while highlighting code
                val nodeToUpdate = current
                if (value < nodeToUpdate.value) {
                    _highlightedCodeLines.value = setOf(CodeLines.INSERT_ELSE, CodeLines.INSERT_GO_LEFT)
                    current = nodeToUpdate.left
                } else {
                    _highlightedCodeLines.value = setOf(CodeLines.INSERT_GO_RIGHT)
                    current = nodeToUpdate.right
                }
                delay(700)
                setNodeState(nodeToUpdate.value, NodeState.NORMAL)
            }

            _root.value = insertRec(_root.value, value)
            setNodeState(value, NodeState.FOUND)
            _highlightedCodeLines.value = setOf(CodeLines.INSERT_RETURN_ROOT)
            delay(1000)
            _nodeStates.value = emptyMap()
            _highlightedCodeLines.value = emptySet()
        }
    }

    private fun insertRec(current: AvlNode?, value: Int): AvlNode {
        if (current == null) {
            return AvlNode(value)
        }

        if (value < current.value) {
            current.left = insertRec(current.left, value)
        } else if (value > current.value) {
            current.right = insertRec(current.right, value)
        } else {
            return current  // Duplicate: do nothing
        }

        current.height = 1 + max(getHeight(current.left), getHeight(current.right))

        val balance = getBalance(current)

        // LL  // FIX: Added !! to assert non-null (guaranteed by AVL logic).
        if (balance > 1 && value < current.left!!.value) {
            _highlightedCodeLines.value = setOf(CodeLines.LL_ROTATE)
            return rightRotate(current)!!
        }

        // RR  // FIX: Added !! to assert non-null (guaranteed by AVL logic).
        if (balance < -1 && value > current.right!!.value) {
            _highlightedCodeLines.value = setOf(CodeLines.RR_ROTATE)
            return leftRotate(current)!!
        }

        // LR  // FIX: Added !! to assert non-null (guaranteed by AVL logic).
        if (balance > 1 && value > current.left!!.value) {
            _highlightedCodeLines.value = setOf(CodeLines.LR_ROTATE)
            current.left = leftRotate(current.left!!)!!
            return rightRotate(current)!!
        }

        // RL  // FIX: Added !! to assert non-null (guaranteed by AVL logic).
        if (balance < -1 && value < current.right!!.value) {
            _highlightedCodeLines.value = setOf(CodeLines.RL_ROTATE)
            current.right = rightRotate(current.right!!)!!
            return leftRotate(current)!!
        }

        return current
    }

    private fun leftRotate(z: AvlNode?): AvlNode? {
        val y = z?.right ?: return null
        val T2 = y.left

        y.left = z
        z.right = T2

        z.height = 1 + max(getHeight(z.left), getHeight(z.right))
        y.height = 1 + max(getHeight(y.left), getHeight(y.right))

        return y
    }

    private fun rightRotate(z: AvlNode?): AvlNode? {
        val y = z?.left ?: return null
        val T3 = y.right

        y.right = z
        z.left = T3

        z.height = 1 + max(getHeight(z.left), getHeight(z.right))
        y.height = 1 + max(getHeight(y.left), getHeight(y.right))

        return y
    }

    private fun getHeight(node: AvlNode?): Int = node?.height ?: 0

    private fun getBalance(node: AvlNode?): Int = node?.let { getHeight(it.left) - getHeight(it.right) } ?: 0

    private fun searchWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var current = _root.value
            while (current != null) {
                setNodeState(current.value, NodeState.COMPARING)
                delay(700)

                if (current.value == value) {
                    setNodeState(current.value, NodeState.FOUND)
                    delay(1500)
                    _nodeStates.value = emptyMap()
                    return@launch
                }
                setNodeState(current.value, NodeState.NORMAL)
                current = if (value < current.value) current.left else current.right
            }
            delay(500)
            _nodeStates.value = emptyMap()
        }
    }

    private fun deleteWithAnimation(value: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var current = _root.value
            while (current != null && current.value != value) {
                setNodeState(current.value, NodeState.COMPARING)
                delay(700)
                setNodeState(current.value, NodeState.NORMAL)
                current = if (value < current.value) current.left else current.right
            }

            if (current != null) {
                setNodeState(current.value, NodeState.FOUND)
                delay(1500) // Pause on the node to be deleted
                _root.value = deleteRec(_root.value, value)
                _nodeStates.value = emptyMap() // Clear states after deletion
            }
        }
    }

    private fun deleteRec(current: AvlNode?, value: Int): AvlNode? {
        if (current == null) {
            return null
        }

        if (value < current.value) {
            current.left = deleteRec(current.left, value)
        } else if (value > current.value) {
            current.right = deleteRec(current.right, value)
        } else {
            if (current.left == null && current.right == null) {
                return null
            }
            if (current.right == null) {
                return current.left
            }
            if (current.left == null) {
                return current.right
            }
            val smallestValue = findSmallestValue(current.right!!)
            val newNode = current.copy(value = smallestValue)
            newNode.right = deleteRec(current.right, smallestValue)
            return newNode
        }

        if (current != null) {
            current.height = 1 + max(getHeight(current.left), getHeight(current.right))

            val balance = getBalance(current)

            // LL
            if (balance > 1 && getBalance(current.left) >= 0) {
                return rightRotate(current)
            }

            // LR
            if (balance > 1 && getBalance(current.left) < 0) {
                current.left = leftRotate(current.left)
                return rightRotate(current)
            }

            // RR
            if (balance < -1 && getBalance(current.right) <= 0) {
                return leftRotate(current)
            }

            // RL
            if (balance < -1 && getBalance(current.right) > 0) {
                current.right = rightRotate(current.right)
                return leftRotate(current)
            }
        }
        return current
    }

    private fun findSmallestValue(root: AvlNode): Int {
        return root.left?.let { findSmallestValue(it) } ?: root.value
    }

    private fun animateTraversal(nodes: List<AvlNode>) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _nodeStates.value = emptyMap()
            delay(200)
            for (node in nodes) {
                setNodeState(node.value, NodeState.VISITED)
                delay(700)
                setNodeState(node.value, NodeState.NORMAL)
            }
            delay(500)
            _nodeStates.value = emptyMap()
        }
    }

    private fun setNodeState(value: Int, state: NodeState) {
        val newStates = _nodeStates.value.toMutableMap()
        newStates[value] = state
        _nodeStates.value = newStates
    }

    // Traversal functions
    private fun inOrderTraversal(): List<AvlNode> {
        val nodes = mutableListOf<AvlNode>()
        inOrderRec(_root.value, nodes)
        return nodes
    }

    private fun inOrderRec(node: AvlNode?, nodes: MutableList<AvlNode>) {
        if (node != null) {
            inOrderRec(node.left, nodes)
            nodes.add(node)
            inOrderRec(node.right, nodes)
        }
    }

    private fun preOrderTraversal(): List<AvlNode> {
        val nodes = mutableListOf<AvlNode>()
        preOrderRec(_root.value, nodes)
        return nodes
    }

    private fun preOrderRec(node: AvlNode?, nodes: MutableList<AvlNode>) {
        if (node != null) {
            nodes.add(node)
            preOrderRec(node.left, nodes)
            preOrderRec(node.right, nodes)
        }
    }

    private fun postOrderTraversal(): List<AvlNode> {
        val nodes = mutableListOf<AvlNode>()
        postOrderRec(_root.value, nodes)
        return nodes
    }

    private fun postOrderRec(node: AvlNode?, nodes: MutableList<AvlNode>) {
        if (node != null) {
            postOrderRec(node.left, nodes)
            postOrderRec(node.right, nodes)
            nodes.add(node)
        }
    }

    private fun levelOrderTraversal(): List<AvlNode> {
        val nodes = mutableListOf<AvlNode>()
        if (_root.value == null) return nodes
        val queue = ArrayDeque<AvlNode>()
        _root.value?.let { queue.add(it) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            nodes.add(node)
            node.left?.let { queue.add(it) }
            node.right?.let { queue.add(it) }
        }
        return nodes
    }

    private fun generatePythonCode(rootNode: AvlNode?, traversal: TraversalType): String {
        return """
# AVL Tree Implementation

class Node:
    def __init__(self, key):
        self.left = None
        self.right = None
        self.val = key
        self.height = 1

def getHeight(node):
    if node is None:
        return 0
    return node.height

def getBalance(node):
    if node is None:
        return 0
    return getHeight(node.left) - getHeight(node.right)

def leftRotate(z):
    y = z.right
    T2 = y.left
    y.left = z
    z.right = T2
    z.height = 1 + max(getHeight(z.left), getHeight(z.right))
    y.height = 1 + max(getHeight(y.left), getHeight(y.right))
    return y

def rightRotate(z):
    y = z.left
    T3 = y.right
    y.right = z
    z.left = T3
    z.height = 1 + max(getHeight(z.left), getHeight(z.right))
    y.height = 1 + max(getHeight(y.left), getHeight(y.right))
    return y

def insert(root, key):
    if root is None:
        return Node(key)
    if root.val < key:
        root.right = insert(root.right, key)
    else:
        root.left = insert(root.left, key)
    root.height = 1 + max(getHeight(root.left), getHeight(root.right))
    balance = getBalance(root)
    if balance > 1 and key < root.left.val:
        return rightRotate(root)
    if balance < -1 and key > root.right.val:
        return leftRotate(root)
    if balance > 1 and key > root.left.val:
        root.left = leftRotate(root.left)
        return rightRotate(root)
    if balance < -1 and key < root.right.val:
        root.right = rightRotate(root.right)
        return leftRotate(root)
    return root

def search(root, key):
    if root is None or root.val == key:
        return root
    if root.val < key:
        return search(root.right, key)
    return search(root.left, key)

# Traversal: ${traversal.name.replace('_', ' ')}
${getTraversalCode(traversal)}

${if (rootNode != null) "\n# Current Tree Structure\n${generateTreeStructure(rootNode)}" else ""}
        """.trimIndent()
    }

    private fun getTraversalCode(traversal: TraversalType): String {
        return when (traversal) {
            TraversalType.IN_ORDER -> """
def inOrder(root):
    if root:
        inOrder(root.left)
        print(root.val)
        inOrder(root.right)
            """.trimIndent()

            TraversalType.PRE_ORDER -> """
def preOrder(root):
    if root:
        print(root.val)
        preOrder(root.left)
        preOrder(root.right)
            """.trimIndent()

            TraversalType.POST_ORDER -> """
def postOrder(root):
    if root:
        postOrder(root.left)
        postOrder(root.right)
        print(root.val)
            """.trimIndent()
        }
    }

    private fun generateTreeStructure(rootNode: AvlNode?): String {
        if (rootNode == null) return ""

        val nodes = levelOrderTraversal()
        val constructionCode = StringBuilder()

        constructionCode.append("# --- AVL Tree Construction ---\n")

        nodes.firstOrNull()?.let { root ->
            constructionCode.append("root = Node(${root.value})\n")
        }

        nodes.drop(1).forEach { node ->
            constructionCode.append("insert(root, ${node.value})\n")
        }

        return constructionCode.toString().trim()
    }

    private fun getBasePythonCode(): String {
        return """
# AVL Tree Implementation

class Node:
    def __init__(self, key):
        self.left = None
        self.right = None
        self.val = key
        self.height = 1

def getHeight(node):
    if node is None:
        return 0
    return node.height

def leftRotate(z):
    y = z.right
    T2 = y.left
    y.left = z
    z.right = T2
    z.height = 1 + max(getHeight(z.left), getHeight(z.right))
    y.height = 1 + max(getHeight(y.left), getHeight(y.right))
    return y

def rightRotate(z):
    y = z.left
    T3 = y.right
    y.right = z
    z.left = T3
    z.height = 1 + max(getHeight(z.left), getHeight(z.right))
    y.height = 1 + max(getHeight(y.left), getHeight(y.right))
    return y

def insert(root, key):
    if root is None:
        return Node(key)
    if root.val < key:
        root.right = insert(root.right, key)
    else:
        root.left = insert(root.left, key)
    root.height = 1 + max(getHeight(root.left), getHeight(root.right))
    balance = getBalance(root)
    if balance > 1 and key < root.left.val:
        return rightRotate(root)
    if balance < -1 and key > root.right.val:
        return leftRotate(root)
    if balance > 1 and key > root.left.val:
        root.left = leftRotate(root.left)
        return rightRotate(root)
    if balance < -1 and key < root.right.val:
        root.right = rightRotate(root.right)
        return leftRotate(root)
    return root
        """.trimIndent()
    }
}