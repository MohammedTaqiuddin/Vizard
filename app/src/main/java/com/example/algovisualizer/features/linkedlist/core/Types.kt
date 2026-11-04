package com.example.algovisualizer.features.linkedlist.core

/**
 * Core model for the unified linked-list visualizer.
 *
 * Design rule (plan 2.1): nodes do NOT carry next/prev. We store list *order* and grid
 * *positions*; pointers are a pure function of (kind, order, nodes). An inconsistent
 * structure -- a.next = b without b.prev = a -- is therefore unrepresentable.
 */

enum class ListKind(val doubly: Boolean, val circular: Boolean) {
    SINGLY(doubly = false, circular = false),
    DOUBLY(doubly = true, circular = false),
    CIRCULAR_SINGLY(doubly = false, circular = true),
    CIRCULAR_DOUBLY(doubly = true, circular = true);

    /** Cells a node occupies: DATA|NEXT, or PREV|DATA|NEXT. */
    val span: Int get() = if (doubly) 3 else 2
}

/** Grid address: column (x), row (y). A node's address is its top-left cell. */
data class Cell(val c: Int, val r: Int) {
    override fun toString() = "($c,$r)"
}

data class LLNode(val id: String, val value: Int, val cell: Cell)

/**
 * How many cells the board has. It lives in the state so pure operations can place a new
 * node without every signature growing two parameters -- and because it genuinely is part
 * of the model: "the board is full" is a fact about this list on this screen.
 *
 * Set from the viewport, never from a constant. See the note on [Grid].
 */
data class Board(val cols: Int, val rows: Int) {
    companion object {
        /** A sensible phone-sized board to lay the first sample out on, before the
         *  viewport has reported the real one. Defined once: two different defaults made
         *  a traced result unequal to the instant result it must match. */
        val Default = Board(24, 12)
    }
}

data class ListState(
    val kind: ListKind,
    val order: List<String> = emptyList(),
    val nodes: Map<String, LLNode> = emptyMap(),
    /** Monotonic id source; kept in state so operations stay pure. */
    val seq: Int = 0,
    val board: Board = Board.Default
) {
    val size: Int get() = order.size
    val isEmpty: Boolean get() = order.isEmpty()

    fun nodeAt(index: Int): LLNode = nodes.getValue(order[index])
    fun cellAt(index: Int): Cell = nodeAt(index).cell
    fun indexOf(id: String): Int = order.indexOf(id)
}

/** Derived pointer view. `null` renders as NULL. */
data class Pointers(
    val head: Cell?,
    val tail: Cell?,
    val next: Map<String, Cell?>,
    val prev: Map<String, Cell?>
)

/** What changed between two states -- drives flashes, the log and the memory table. */
sealed interface Change {
    data class Head(val from: Cell?, val to: Cell?) : Change
    data class Tail(val from: Cell?, val to: Cell?) : Change
    data class Next(val nodeId: String, val from: Cell?, val to: Cell?) : Change
    data class Prev(val nodeId: String, val from: Cell?, val to: Cell?) : Change
    data class Addr(val nodeId: String, val from: Cell, val to: Cell) : Change
}

/** Result of one operation: the new state plus everything that changed. */
data class Applied(val state: ListState, val changes: List<Change>)
