package com.example.algovisualizer.features.linkedlist.core

/**
 * One trace drives everything (plan 12.1). An operation is a generator that yields one
 * [Step] per meaningful line of code executed; the board, the variables panel, the code tab
 * and the narration are all just views of the current step, so they cannot drift apart.
 */

enum class PointerField { NEXT, PREV }

sealed interface Effect {
    /** Node pops in at its cell. */
    data class Create(val id: String) : Effect
    /** Node glows as a pointer variable lands on it. */
    data class Visit(val id: String) : Effect
    /** A variable chip slides along the arrow from one node to the next. */
    data class Travel(val varName: String, val from: String, val to: String) : Effect
    /** An arrow's end swings from one target to another (null = NULL marker). */
    data class Retarget(
        val id: String,
        val field: PointerField,
        val from: String?,
        val to: String?
    ) : Effect
    /** Search: tick or cross on the DATA box. */
    data class Compare(val id: String, val result: Boolean) : Effect
    /** Node fades out; its cell is released. */
    data class Free(val id: String) : Effect
}

data class Step(
    /** Code line id -- the same ids appear in every language listing (plan 12.6). */
    val line: String,
    /** Full picture of memory after this line ran, so stepping back can tween in reverse. */
    val frame: Frame,
    val vars: Map<String, String?> = emptyMap(),
    val effects: List<Effect> = emptyList(),
    val note: String = ""
)

// ---- Frame helpers (plan 12.7 frame.ts) ---------------------------------

fun Frame.withPointer(id: String, field: PointerField, target: String?): Frame = when (field) {
    PointerField.NEXT -> copy(next = next + (id to target))
    PointerField.PREV -> copy(prev = prev + (id to target))
}

fun Frame.withNode(node: FrameNode): Frame = copy(
    nodes = nodes + (node.id to node),
    order = if (node.id in order) order else order + node.id
)

fun Frame.withStatus(id: String, status: NodeStatus): Frame {
    val node = nodes[id] ?: return this
    return copy(nodes = nodes + (id to node.copy(status = status)))
}

fun Frame.withHead(id: String?): Frame = copy(head = id)

fun Frame.withTail(id: String?): Frame = copy(tail = id)

fun Frame.dropNode(id: String): Frame = copy(
    nodes = nodes - id,
    next = next - id,
    prev = prev - id,
    order = order - id
)

/** Value of a node, for narration. */
fun Frame.valueOf(id: String?): String = id?.let { nodes[it]?.value?.toString() } ?: "NULL"

/** Address of a node, for the variables panel. */
fun Frame.addressOf(id: String?): String? = id?.let { nodes[it]?.cell?.toString() }
