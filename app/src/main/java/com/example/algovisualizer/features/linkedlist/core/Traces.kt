package com.example.algovisualizer.features.linkedlist.core

/**
 * Step traces for each operation (plan 12.4). Line ids match the code listings, so one
 * trace drives the canvas, the variables panel and every language tab at once.
 *
 * The last frame of every trace must equal what the instant operation produces -- asserted
 * in tests (plan 12.8).
 */

private fun Frame.addr(id: String?): String? = addressOf(id)

/** Insert at [index]: 0 = head, size = tail. Mirrors [insertAt] exactly. */
fun ListState.traceInsertAt(index: Int, value: Int): List<Step> {
    require(index in 0..size) { "index $index out of 0..$size" }
    val cell = freeSpot() ?: throw GridFullException()
    val newId = "n$seq"

    val steps = mutableListOf<Step>()
    var f = toFrame()
    val oldHead = f.head
    val oldTail = f.tail

    // 1. allocate
    f = f.withNode(FrameNode(newId, value, cell, NodeStatus.NEW))
        .withPointer(newId, PointerField.NEXT, null)
    if (kind.doubly) f = f.withPointer(newId, PointerField.PREV, null)
    steps += Step(
        line = "create",
        frame = f,
        vars = mapOf("newNode" to cell.toString()),
        effects = listOf(Effect.Create(newId)),
        note = "Allocate a node holding $value. Its pointers are NULL for now."
    )

    if (index == 0) {
        steps += Step("head-case", f, mapOf("newNode" to cell.toString()),
            note = "index = 0, so the new node becomes the head.")

        f = f.withPointer(newId, PointerField.NEXT, oldHead)
        steps += Step(
            line = "link-new", frame = f,
            vars = mapOf("newNode" to cell.toString(), "head" to f.addr(oldHead)),
            effects = listOf(Effect.Retarget(newId, PointerField.NEXT, null, oldHead)),
            note = "newNode.next points at the old head."
        )

        if (kind.doubly && oldHead != null) {
            f = f.withPointer(oldHead, PointerField.PREV, newId)
            steps += Step(
                line = "link-back", frame = f,
                effects = listOf(Effect.Retarget(oldHead, PointerField.PREV, f.prev[oldHead], newId)),
                note = "The old head's prev points back at newNode."
            )
        }

        f = f.withHead(newId)
        if (oldHead == null) f = f.withTail(newId)
        steps += Step(
            line = "set-head", frame = f,
            vars = mapOf("head" to cell.toString()),
            note = "HEAD now refers to ${cell}."
        )

        if (kind.circular) {
            val tail = f.tail
            if (tail != null) {
                f = f.withPointer(tail, PointerField.NEXT, newId)
                if (kind.doubly) f = f.withPointer(newId, PointerField.PREV, tail)
                steps += Step(
                    line = "close-ring", frame = f,
                    effects = listOf(Effect.Retarget(tail, PointerField.NEXT, oldHead, newId)),
                    note = "The ring closes: tail.next follows the new head."
                )
            }
        }
    } else {
        steps += Step("head-case", f, note = "index ≠ 0, so walk to the node before it.")

        var curr = oldHead!!
        f = f.withStatus(curr, NodeStatus.NORMAL)
        steps += Step(
            line = "init", frame = f,
            vars = mapOf("curr" to f.addr(curr), "k" to "0"),
            effects = listOf(Effect.Visit(curr)),
            note = "curr starts at the head."
        )

        repeat(index - 1) { k ->
            val nextId = f.next[curr]!!
            steps += Step(
                line = "loop", frame = f,
                vars = mapOf("curr" to f.addr(curr), "k" to "${k + 1}"),
                note = "Not there yet — step forward."
            )
            steps += Step(
                line = "advance", frame = f,
                vars = mapOf("curr" to f.addr(nextId), "k" to "${k + 1}"),
                effects = listOf(Effect.Travel("curr", curr, nextId)),
                note = "curr follows its own next pointer."
            )
            curr = nextId
        }

        val after = f.next[curr]
        f = f.withPointer(newId, PointerField.NEXT, after)
        steps += Step(
            line = "link-new", frame = f,
            vars = mapOf("curr" to f.addr(curr), "newNode" to cell.toString()),
            effects = listOf(Effect.Retarget(newId, PointerField.NEXT, null, after)),
            note = "newNode.next takes over curr's old target. Both point there for a moment."
        )

        if (kind.doubly) {
            f = f.withPointer(newId, PointerField.PREV, curr)
            if (after != null) f = f.withPointer(after, PointerField.PREV, newId)
            steps += Step(
                line = "link-back", frame = f,
                effects = listOfNotNull(
                    Effect.Retarget(newId, PointerField.PREV, null, curr),
                    after?.let { Effect.Retarget(it, PointerField.PREV, curr, newId) }
                ),
                note = "The prev pointers are rewired to match."
            )
        }

        f = f.withPointer(curr, PointerField.NEXT, newId)
        if (curr == oldTail && !kind.circular) f = f.withTail(newId)
        if (curr == oldTail && kind.circular) f = f.withTail(newId)
        steps += Step(
            line = "link-prev", frame = f,
            vars = mapOf("curr" to f.addr(curr)),
            effects = listOf(Effect.Retarget(curr, PointerField.NEXT, after, newId)),
            note = "curr.next swings to the new node. The list is linked again."
        )
    }

    f = f.withStatus(newId, NodeStatus.NORMAL)
    steps += Step("done", f, note = "Inserted $value at index $index.")
    return steps
}

/** Delete at [index], showing the bypass then the free (plan 12.4). */
fun ListState.traceDeleteAt(index: Int): List<Step> {
    require(index in 0 until size) { "index $index out of 0..<$size" }
    val steps = mutableListOf<Step>()
    var f = toFrame()
    val target = order[index]

    if (index == 0) {
        val newHead = f.next[target]
        steps += Step(
            line = "head-case", frame = f,
            vars = mapOf("head" to f.addr(target)),
            effects = listOf(Effect.Visit(target)),
            note = "The head itself is being removed."
        )
        f = f.withHead(if (newHead == target) null else newHead)
        if (kind.doubly && newHead != null && newHead != target) {
            f = f.withPointer(newHead, PointerField.PREV, if (kind.circular) f.tail else null)
        }
        if (kind.circular) {
            val tail = f.tail
            if (tail != null && tail != target && newHead != null) {
                f = f.withPointer(tail, PointerField.NEXT, newHead)
            }
        }
        steps += Step(
            line = "move-head", frame = f,
            vars = mapOf("head" to f.addr(f.head)),
            note = "HEAD moves to ${f.addr(f.head) ?: "NULL"}."
        )
    } else {
        var prev = f.head!!
        steps += Step(
            line = "init", frame = f,
            vars = mapOf("prev" to f.addr(prev)),
            effects = listOf(Effect.Visit(prev)),
            note = "prev starts at the head."
        )
        repeat(index - 1) {
            val nextId = f.next[prev]!!
            steps += Step(
                line = "advance", frame = f,
                vars = mapOf("prev" to f.addr(nextId)),
                effects = listOf(Effect.Travel("prev", prev, nextId)),
                note = "Walk to the node before the target."
            )
            prev = nextId
        }

        f = f.withStatus(target, NodeStatus.DETACHED)
        steps += Step(
            line = "find-target", frame = f,
            vars = mapOf("prev" to f.addr(prev), "target" to f.addr(target)),
            effects = listOf(Effect.Visit(target)),
            note = "target is the node to remove."
        )

        val after = f.next[target]
        f = f.withPointer(prev, PointerField.NEXT, after)
        if (kind.doubly && after != null) f = f.withPointer(after, PointerField.PREV, prev)
        if (target == f.tail) f = f.withTail(prev)
        steps += Step(
            line = "bypass", frame = f,
            effects = listOf(Effect.Retarget(prev, PointerField.NEXT, target, after)),
            note = "prev.next skips past the target. Nothing points at it now."
        )
    }

    f = f.withStatus(target, NodeStatus.FREED)
    steps += Step(
        line = "free", frame = f,
        effects = listOf(Effect.Free(target)),
        note = "The node is released and its cell is free again."
    )
    return steps + Step("done", f.dropNode(target), note = "Removed index $index.")
}

/** Search by value: compare, then follow the pointer (plan 12.4). */
fun ListState.traceSearch(value: Int): List<Step> {
    val steps = mutableListOf<Step>()
    val f = toFrame()
    if (f.head == null) {
        return listOf(Step("empty", f, note = "The list is empty — nothing to search."))
    }

    var curr: String? = f.head
    var idx = 0
    steps += Step(
        line = "init", frame = f,
        vars = mapOf("curr" to f.addr(curr), "idx" to "0"),
        effects = listOf(Effect.Visit(curr!!)),
        note = "curr starts at the head."
    )

    while (curr != null && idx < order.size) {
        val node = f.nodes.getValue(curr)
        val hit = node.value == value
        steps += Step(
            line = "compare", frame = f,
            vars = mapOf("curr" to f.addr(curr), "idx" to "$idx"),
            effects = listOf(Effect.Compare(curr, hit)),
            note = if (hit) "${node.value} = $value — found it." else "${node.value} ≠ $value."
        )
        if (hit) {
            return steps + Step(
                "found", f,
                vars = mapOf("curr" to f.addr(curr), "idx" to "$idx"),
                note = "Found $value at index $idx."
            )
        }
        val nextId = f.next[curr]
        if (nextId == null || nextId == f.head) break
        steps += Step(
            line = "advance", frame = f,
            vars = mapOf("curr" to f.addr(nextId), "idx" to "${idx + 1}"),
            effects = listOf(Effect.Travel("curr", curr, nextId)),
            note = "Follow curr.next."
        )
        curr = nextId
        idx++
    }

    return steps + Step("not-found", f, note = "$value is not in the list.")
}
