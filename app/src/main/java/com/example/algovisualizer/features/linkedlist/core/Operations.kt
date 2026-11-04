package com.example.algovisualizer.features.linkedlist.core

/**
 * Operations (plan 1). None of these touch pointers -- they only reorder [ListState.order]
 * and place nodes on the grid, so all four list kinds share one implementation.
 *
 * Each returns the new state; wrap in [perform] to get the change list too.
 */

class GridFullException : IllegalStateException("No free cell on the grid")

private fun ListState.withNode(node: LLNode, index: Int): ListState = copy(
    order = order.toMutableList().apply { add(index, node.id) },
    nodes = nodes + (node.id to node),
    seq = seq + 1
)

/** Insert [value] at [index] (0 = head, size = tail). */
fun ListState.insertAt(index: Int, value: Int, cell: Cell? = null): ListState {
    require(index in 0..size) { "index $index out of 0..$size" }
    val spot = cell ?: freeSpot() ?: throw GridFullException()
    require(fits(spot)) { "cell $spot is taken or off the board" }
    return withNode(LLNode(id = "n$seq", value = value, cell = spot), index)
}

fun ListState.insertHead(value: Int, cell: Cell? = null): ListState = insertAt(0, value, cell)

fun ListState.insertTail(value: Int, cell: Cell? = null): ListState = insertAt(size, value, cell)

fun ListState.deleteAt(index: Int): ListState {
    require(index in 0 until size) { "index $index out of 0..<$size" }
    val id = order[index]
    return copy(order = order - id, nodes = nodes - id)
}

fun ListState.deleteHead(): ListState = deleteAt(0)

fun ListState.deleteTail(): ListState = deleteAt(size - 1)

fun ListState.deleteById(id: String): ListState {
    val index = indexOf(id)
    require(index >= 0) { "no node $id" }
    return deleteAt(index)
}

/** First index holding [value], or -1. Identity is the id, so duplicates are fine. */
fun ListState.indexOfValue(value: Int): Int = order.indexOfFirst { nodes.getValue(it).value == value }

/** Reverse the logical order. Addresses stay put -- only pointers flip. */
fun ListState.reverse(): ListState = copy(order = order.reversed())

/** Move a node to a new address. Pointers that reference it update themselves. */
fun ListState.moveNode(id: String, cell: Cell): ListState {
    val node = nodes[id] ?: error("no node $id")
    require(fits(cell, ignoreId = id)) { "cell $cell is taken or off the board" }
    return copy(nodes = nodes + (id to node.copy(cell = cell)))
}

fun ListState.clear(): ListState = copy(order = emptyList(), nodes = emptyMap())

/** A seed list for each kind, laid out left to right. */
fun sampleList(kind: ListKind, values: List<Int> = listOf(12, 7, 30, 4)): ListState =
    values.fold(ListState(kind = kind)) { acc, v -> acc.insertTail(v) }
