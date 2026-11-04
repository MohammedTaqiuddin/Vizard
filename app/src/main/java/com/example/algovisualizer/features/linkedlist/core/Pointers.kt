package com.example.algovisualizer.features.linkedlist.core

/**
 * Pointers are derived, never stored (plan 2.1). Only this file knows what each list
 * kind means; every operation and every view works for all four kinds for free.
 */
fun ListState.pointersOf(): Pointers {
    val n = order.size
    if (n == 0) return Pointers(head = null, tail = null, next = emptyMap(), prev = emptyMap())

    val next = LinkedHashMap<String, Cell?>(n)
    val prev = LinkedHashMap<String, Cell?>(if (kind.doubly) n else 0)

    order.forEachIndexed { i, id ->
        next[id] = when {
            i < n - 1 -> cellAt(i + 1)
            kind.circular -> cellAt(0)       // tail wraps to head
            else -> null                      // tail.next = NULL
        }
        if (kind.doubly) {
            prev[id] = when {
                i > 0 -> cellAt(i - 1)
                kind.circular -> cellAt(n - 1)  // head.prev wraps to tail
                else -> null                     // head.prev = NULL
            }
        }
    }
    return Pointers(head = cellAt(0), tail = cellAt(n - 1), next = next, prev = prev)
}

/**
 * Diff two states into the exact set of changes. Node identity is the id, so a node that
 * merely moved produces an [Change.Addr] plus whatever pointers referenced it.
 */
fun diffStates(before: ListState, after: ListState): List<Change> {
    val a = before.pointersOf()
    val b = after.pointersOf()
    val changes = mutableListOf<Change>()

    if (a.head != b.head) changes += Change.Head(a.head, b.head)
    if (a.tail != b.tail) changes += Change.Tail(a.tail, b.tail)

    // Address changes, in list order so the log reads top-to-bottom.
    after.order.forEach { id ->
        val from = before.nodes[id]?.cell ?: return@forEach
        val to = after.nodes.getValue(id).cell
        if (from != to) changes += Change.Addr(id, from, to)
    }

    // Pointer fields. A node present in only one state still reports its field, so
    // inserts and deletes show the links they create or drop.
    (after.order + before.order.filter { it !in after.nodes }).distinct().forEach { id ->
        val nFrom = if (before.nodes.containsKey(id)) a.next[id] else null
        val nTo = if (after.nodes.containsKey(id)) b.next[id] else null
        if (before.nodes.containsKey(id) != after.nodes.containsKey(id) || nFrom != nTo) {
            if (nFrom != nTo) changes += Change.Next(id, nFrom, nTo)
        }
        if (before.kind.doubly || after.kind.doubly) {
            val pFrom = if (before.nodes.containsKey(id)) a.prev[id] else null
            val pTo = if (after.nodes.containsKey(id)) b.prev[id] else null
            if (pFrom != pTo) changes += Change.Prev(id, pFrom, pTo)
        }
    }
    return changes
}

/** Every mutation goes through here, so feedback is uniform across all list kinds. */
fun ListState.perform(op: (ListState) -> ListState): Applied {
    val next = op(this)
    return Applied(next, diffStates(this, next))
}

/**
 * Follow `next` from the head the way a program would, using only derived pointers.
 * Returns the ids visited. Capped at `size` steps so a circular list terminates.
 */
fun ListState.walk(): List<String> {
    if (isEmpty) return emptyList()
    val byCell = nodes.values.associateBy { it.cell }
    val p = pointersOf()
    val visited = mutableListOf<String>()
    var current: String? = order[0]
    while (current != null && visited.size < order.size) {
        visited += current
        val target = p.next[current] ?: break
        current = byCell[target]?.id
        if (current == order[0]) break   // circular: back at head
    }
    return visited
}
