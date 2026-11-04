package com.example.algovisualizer.features.linkedlist.core

/**
 * A full picture of memory at one instant (plan 12.1).
 *
 * [ListState] derives pointers from `order`, which is right for committed states but cannot
 * express the moments *between* two lines of code -- e.g. during insert, `newNode.next`
 * already points at `curr.next` while `curr.next` still points at the old node. Those
 * half-linked states are exactly what the animation needs to show, so a Frame stores
 * pointers explicitly, by node id.
 *
 * The render layer draws Frames only, never ListState, so the step engine (milestone 2a)
 * can hand it intermediate states without any change to Board/NodeView/Wires.
 */

enum class NodeStatus { NORMAL, NEW, DETACHED, FREED }

data class FrameNode(
    val id: String,
    val value: Int,
    val cell: Cell,
    val status: NodeStatus = NodeStatus.NORMAL
)

data class Frame(
    val kind: ListKind,
    val nodes: Map<String, FrameNode>,
    /** node id -> node id, or null for NULL. Explicit, unlike ListState. */
    val next: Map<String, String?>,
    val prev: Map<String, String?>,
    val head: String?,
    val tail: String?,
    /** Draw order, so rendering is stable frame to frame. */
    val order: List<String>
) {
    fun cellOf(id: String?): Cell? = id?.let { nodes[it]?.cell }
    val isEmpty: Boolean get() = nodes.isEmpty()
}

/** Committed state -> frame. The common case: every pointer follows from `order`. */
fun ListState.toFrame(): Frame {
    val n = order.size
    val nextIds = LinkedHashMap<String, String?>(n)
    val prevIds = LinkedHashMap<String, String?>(if (kind.doubly) n else 0)

    order.forEachIndexed { i, id ->
        nextIds[id] = when {
            i < n - 1 -> order[i + 1]
            kind.circular && n > 0 -> order[0]
            else -> null
        }
        if (kind.doubly) {
            prevIds[id] = when {
                i > 0 -> order[i - 1]
                kind.circular && n > 0 -> order[n - 1]
                else -> null
            }
        }
    }

    return Frame(
        kind = kind,
        nodes = nodes.mapValues { (_, node) -> FrameNode(node.id, node.value, node.cell) },
        next = nextIds,
        prev = prevIds,
        head = order.firstOrNull(),
        tail = order.lastOrNull(),
        order = order
    )
}

/**
 * Frame -> committed state. Used to close a trace: the last frame of every animation must
 * equal what the instant operation produced (asserted in tests, plan 12.8).
 * Nodes still marked FREED are dropped, and order is recovered by following `next`.
 */
fun Frame.toListState(seq: Int = 0, board: Board = Board.Default): ListState {
    val live = nodes.filterValues { it.status != NodeStatus.FREED }
    val recovered = mutableListOf<String>()
    var current = head
    while (current != null && current in live && recovered.size < live.size) {
        recovered += current
        current = next[current]
        if (current == head) break
    }
    // Anything unreachable (a detached node mid-operation) keeps its draw order at the end.
    val rest = order.filter { it in live && it !in recovered }
    val finalOrder = recovered + rest
    return ListState(
        kind = kind,
        order = finalOrder,
        nodes = finalOrder.associateWith { id ->
            val f = live.getValue(id)
            LLNode(f.id, f.value, f.cell)
        },
        seq = seq,
        board = board
    )
}

