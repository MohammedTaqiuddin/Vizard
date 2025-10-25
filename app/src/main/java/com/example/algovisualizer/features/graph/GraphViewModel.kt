package com.example.algovisualizer.features.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.core.data.OperationResult
import com.example.algovisualizer.core.ui.BaseVisualizerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GraphViewModel : BaseVisualizerViewModel() {

    private var animationJob: Job? = null
    private var selectedNodeId by mutableStateOf<String?>(null)
    private var nextNodeId = 'A'

    private val _nodes = MutableStateFlow<Map<String, GraphNode>>(emptyMap())
    val nodes: StateFlow<Map<String, GraphNode>> = _nodes.asStateFlow()

    private val _edges = MutableStateFlow<Set<GraphEdge>>(emptySet())
    val edges: StateFlow<Set<GraphEdge>> = _edges.asStateFlow()

    private val _nodeStates = MutableStateFlow<Map<String, GraphNodeState>>(emptyMap())
    val nodeStates: StateFlow<Map<String, GraphNodeState>> = _nodeStates.asStateFlow()

    private val _adjacencyList = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    private val _interactionMode = MutableStateFlow(InteractionMode.PAN_ZOOM)
    val interactionMode: StateFlow<InteractionMode> = _interactionMode.asStateFlow()

    private val _pythonCode = MutableStateFlow("")
    val pythonCode: StateFlow<String> = _pythonCode.asStateFlow()

    private val _highlightedCodeLines = MutableStateFlow<Set<Int>>(emptySet())
    val highlightedCodeLines: StateFlow<Set<Int>> = _highlightedCodeLines.asStateFlow()

    private val _traversalResult = MutableStateFlow<List<String>>(emptyList())
    val traversalResult: StateFlow<List<String>> = _traversalResult.asStateFlow()

    override suspend fun onOperation(operationName: String, vararg params: Any): OperationResult {
        return try {
            when (operationName) {
                "bfs" -> startBfs(params[0] as String)
                "dfs" -> startDfs(params[0] as String)
                "add_node" -> addNode(params[0] as Offset)
                "select_node" -> handleNodeSelection(params[0] as String)
                "reset" -> resetGraphVisuals()
                "clear" -> resetGraph()
                "sample" -> setupSampleGraph()
            }
            OperationResult.Success(emptyList())
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Unknown error")
        }
    }

    fun setInteractionMode(mode: InteractionMode) {
        selectedNodeId?.let { setNodeState(it, GraphNodeState.IDLE) }
        selectedNodeId = null
        _interactionMode.value = mode
    }

    private fun handleNodeSelection(nodeId: String) {
        val currentlySelected = selectedNodeId
        if (currentlySelected == null) {
            selectedNodeId = nodeId
            setNodeState(nodeId, GraphNodeState.SELECTED)
        } else {
            if (currentlySelected != nodeId) addEdge(currentlySelected, nodeId)
            setNodeState(currentlySelected, GraphNodeState.IDLE)
            selectedNodeId = null
        }
    }

    private fun addNode(offset: Offset) {
        val id = nextNodeId.toString()
        addNode(id, id, offset)
        nextNodeId++
    }

    private fun addNode(id: String, label: String, offset: Offset) {
        _nodes.update { it + (id to GraphNode(id, label, offset)) }
        _adjacencyList.update { it + (id to emptyList()) }
    }

    private fun addEdge(from: String, to: String) {
        if (nodes.value.containsKey(from) && nodes.value.containsKey(to)) {
            val newEdge1 = GraphEdge(from, to)
            val newEdge2 = GraphEdge(to, from)
            _edges.update { it + newEdge1 + newEdge2 }

            _adjacencyList.update { currentAdj ->
                val updatedFrom = currentAdj.getOrElse(from) { emptyList() } + to
                val updatedTo = currentAdj.getOrElse(to) { emptyList() } + from
                currentAdj + (from to updatedFrom) + (to to updatedTo)
            }
        }
    }

    private fun startBfs(startNodeId: String) {
        animationJob?.cancel()
        _pythonCode.value = generateBfsCode()
        animationJob = viewModelScope.launch {
            resetGraphVisuals()
            delay(300)
            if (!_nodes.value.containsKey(startNodeId)) return@launch

            val queue = ArrayDeque<String>()
            val visited = mutableSetOf<String>()

            _highlightedCodeLines.value = setOf(2, 3) // queue, visited
            delay(500)

            queue.add(startNodeId)
            visited.add(startNodeId)
            setNodeState(startNodeId, GraphNodeState.IN_QUEUE)
            _highlightedCodeLines.value = setOf(5, 6) // queue.add, visited.add
            delay(700)

            _highlightedCodeLines.value = setOf(8) // while loop
            delay(500)
            while (queue.isNotEmpty()) {
                val currentId = queue.removeFirst()
                _traversalResult.update { it + currentId }
                setNodeState(currentId, GraphNodeState.CURRENT)
                _highlightedCodeLines.value = setOf(9) // queue.pop
                delay(700)

                _highlightedCodeLines.value = setOf(11) // for neighbor
                delay(500)
                _adjacencyList.value[currentId]?.forEach { neighborId ->
                    _highlightedCodeLines.value = setOf(12) // if neighbor not visited
                    delay(500)
                    if (neighborId !in visited) {
                        visited.add(neighborId)
                        setNodeState(neighborId, GraphNodeState.IN_QUEUE)
                        queue.add(neighborId)
                        _highlightedCodeLines.value = setOf(13, 14, 15) // visited.add, set state, queue.add
                        delay(700)
                    }
                }
                setNodeState(currentId, GraphNodeState.VISITED)
                _highlightedCodeLines.value = setOf(8) // Back to while
                delay(700)
            }
            _highlightedCodeLines.value = emptySet()
        }
    }

    private fun startDfs(startNodeId: String) {
        animationJob?.cancel()
        _pythonCode.value = generateDfsCode()
        animationJob = viewModelScope.launch {
            resetGraphVisuals()
            delay(300)
            if (!_nodes.value.containsKey(startNodeId)) return@launch

            val visited = mutableSetOf<String>()
            _highlightedCodeLines.value = setOf(2) // visited = set()
            delay(500)
            dfsRecursiveWithAnimation(startNodeId, visited)
            _highlightedCodeLines.value = emptySet()
        }
    }

    private suspend fun dfsRecursiveWithAnimation(nodeId: String, visited: MutableSet<String>) {
        visited.add(nodeId)
        _traversalResult.update { it + nodeId }
        setNodeState(nodeId, GraphNodeState.CURRENT)
        _highlightedCodeLines.value = setOf(5, 6) // visited.add, set state
        delay(700)

        _highlightedCodeLines.value = setOf(8) // for neighbor
        delay(500)
        _adjacencyList.value[nodeId]?.forEach { neighborId ->
            _highlightedCodeLines.value = setOf(9) // if neighbor not visited
            delay(500)
            if (neighborId !in visited) {
                setNodeState(neighborId, GraphNodeState.IN_QUEUE)
                _highlightedCodeLines.value = setOf(10) // recursive call
                delay(700)
                dfsRecursiveWithAnimation(neighborId, visited)
                _highlightedCodeLines.value = setOf(9) // return from recursion
                delay(500)
            }
        }

        setNodeState(nodeId, GraphNodeState.VISITED)
        _highlightedCodeLines.value = setOf(5) // return from function
        delay(700)
    }

    private fun setNodeState(nodeId: String, state: GraphNodeState) {
        _nodeStates.update { it + (nodeId to state) }
    }

    private fun resetGraph() {
        nextNodeId = 'A'
        _nodes.value = emptyMap()
        _edges.value = emptySet()
        _adjacencyList.value = emptyMap()
        resetGraphVisuals()
    }

    private fun resetGraphVisuals() {
        animationJob?.cancel()
        selectedNodeId?.let { setNodeState(it, GraphNodeState.IDLE) }
        selectedNodeId = null
        _nodeStates.value = emptyMap()
        _highlightedCodeLines.value = emptySet()
        _traversalResult.value = emptyList()
    }

    private fun setupSampleGraph() {
        viewModelScope.launch {
            resetGraph()
            addNode("A", "A", Offset(450f, 200f))
            addNode("B", "B", Offset(650f, 350f))
            addNode("C", "C", Offset(600f, 550f))
            addNode("D", "D", Offset(300f, 550f))
            addNode("E", "E", Offset(250f, 350f))
            addEdge("A", "B")
            addEdge("A", "E")
            addEdge("B", "C")
            addEdge("C", "D")
            addEdge("D", "E")
            // Set initial code to BFS as a default
            _pythonCode.value = generateBfsCode()
        }
    }

    private fun generateBfsCode(): String = """
# Breadth-First Search (BFS)
def bfs(graph, start_node):
    queue = []
    visited = set()

    queue.append(start_node)
    visited.add(start_node)

    while queue:
        current_node = queue.pop(0)
        print(current_node)

        for neighbor in graph[current_node]:
            if neighbor not in visited:
                visited.add(neighbor)
                queue.append(neighbor)
    """".trimIndent()

    private fun generateDfsCode(): String = """
# Depth-First Search (DFS)
def dfs(graph, start_node):
    visited = set()
    _dfs_recursive(graph, start_node, visited)

def _dfs_recursive(graph, node, visited):
    visited.add(node)
    print(node)

    for neighbor in graph[node]:
        if neighbor not in visited:
            _dfs_recursive(graph, neighbor, visited)
    """".trimIndent()
}
