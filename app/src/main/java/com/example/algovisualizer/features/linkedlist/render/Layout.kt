package com.example.algovisualizer.features.linkedlist.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.algovisualizer.features.linkedlist.core.Frame
import com.example.algovisualizer.features.linkedlist.core.gap
import kotlin.math.max

/**
 * Where the nodes go. Two answers, because a linked list has two things to teach and they
 * want opposite layouts:
 *
 *  - [ChainLayout]  position means *list order*. What a linked list IS: a chain you walk.
 *  - [MemoryLayout] position means *address*. WHY it isn't an array: storage is scattered
 *                   and the order lives in the pointers, not in the addresses.
 *
 * The board draws at `lerp(chain, memory, t)` and animates t on the toggle, so switching
 * views is itself the lesson: the nodes fly apart, and the list is still the same list.
 */
interface ListLayout {
    fun positions(frame: Frame, m: Metrics): Map<String, Offset>
}

/** Rows a chain leaves between its wrapped lines, so wrap edges have a channel to use. */
const val CHAIN_ROW_STRIDE = 2

/**
 * Columns between neighbours in chain view. One is enough: the two arrows are separated
 * by lane, not by width, so chain view does not need the wider gap memory view does.
 */
const val CHAIN_GAP = 1

/**
 * A wrap is worth taking only if it buys this much of the best scale available. Below it,
 * the extra size is not worth breaking the line; above it, the nodes were simply too small.
 * One line is what a chain *wants* to be -- wrapping is the concession.
 */
private const val WRAP_WORTH_IT = 0.72f

/** Chrome around the chain, in cells: labels and HEAD/TAIL chips. */
private const val CHROME_X = 1.8f
private const val CHROME_Y = 2.3f

/**
 * How many nodes go on one line of the chain.
 *
 * Not the memory board's column count, and not a constant: chain view scales to fit its own
 * contents, so the right answer depends on the shape of the canvas. A phone in portrait has
 * height going spare and wants a wrap; a tablet or a landscape window has the width to keep
 * the chain on one line at the same node size. Try every line length, keep the one that
 * draws the biggest node, and break the tie toward fewer lines.
 */
fun chainPerLine(frame: Frame, m: Metrics, canvas: Size, maxScale: Float = MAX_SCALE): Int {
    val n = frame.order.size
    if (n <= 1 || canvas.width <= 0f || canvas.height <= 0f) return max(1, n)
    val span = frame.kind.span

    fun scaleFor(perLine: Int): Float {
        val wide = perLine * span + (perLine - 1) * CHAIN_GAP + CHROME_X
        val lines = (n + perLine - 1) / perLine
        val tall = (lines - 1) * CHAIN_ROW_STRIDE + 1 + CHROME_Y
        return minOf(canvas.width / (wide * m.cell), canvas.height / (tall * m.cell))
            .coerceAtMost(maxScale)
    }

    val options = (1..n).map { it to scaleFor(it) }
    val best = options.maxOf { it.second }
    return options.last { it.second >= best * WRAP_WORTH_IT }.first
}

/**
 * Index-based: node i goes at slot i, wrapping to the next line in reading order once a
 * line is full. Addresses are ignored entirely.
 */
class ChainLayout(perLine: Int) : ListLayout {

    private val perLine = max(1, perLine)

    override fun positions(frame: Frame, m: Metrics): Map<String, Offset> {
        val step = frame.kind.span + CHAIN_GAP
        return frame.order.withIndex().associate { (i, id) ->
            id to Offset(m.x((i % perLine) * step), m.y((i / perLine) * CHAIN_ROW_STRIDE))
        }
    }
}

/** Address-based: the node sits wherever its cell says, which is what dragging edits. */
object MemoryLayout : ListLayout {
    override fun positions(frame: Frame, m: Metrics): Map<String, Offset> =
        frame.nodes.mapValues { (_, node) -> m.originOf(node.cell) }
}

/**
 * Both layouts blended. [t] = 0 is pure chain, 1 is pure memory. Every node in the frame
 * gets a position from both sides -- a node missing from one (a freed node mid-trace is in
 * `nodes` but may be dropped from `order`) falls back to the other rather than vanishing.
 */
fun morphedPositions(frame: Frame, m: Metrics, perLine: Int, t: Float): Map<String, Offset> {
    val memory = MemoryLayout.positions(frame, m)
    if (t >= 1f) return memory
    val chain = ChainLayout(perLine).positions(frame, m)
    if (t <= 0f) return frame.nodes.keys.associateWith { chain[it] ?: memory.getValue(it) }

    return frame.nodes.keys.associateWith { id ->
        val a = chain[id] ?: memory.getValue(id)
        val b = memory.getValue(id)
        Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }
}
